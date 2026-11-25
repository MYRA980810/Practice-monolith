// Stateless review of an IVS Chat message plus best-effort hand-off of allowed messages to SQS.
// Pure module: no AWS SDK import, so it is unit-testable without node_modules (see index.mjs).

const PHONE_MIN_DIGITS = 10; // MX numbers are 10 digits.

// Explicit URLs and "www." hosts: always a link, whatever the TLD.
const EXPLICIT_LINK = /(?:\bhttps?:\/\/|\bwww\.)[a-z0-9-]/i;

// Bare domains ("mitienda.com", "bit.ly/x", "tienda.com.mx") only on a curated TLD list. TLDs that
// are also short Spanish words (me, es, do, co, us, pa, la, lo, se, te, tu, ya, no, mi, si, ve, uy...)
// are left out on purpose, so "ok.me interesa" or "roja.Me la envias" are not links; those domains
// are still blocked when written with http(s):// or www.
const BARE_DOMAIN_TLDS = [
  'com', 'net', 'org', 'info', 'biz', 'io', 'ly', 'gg', 'tv', 'app', 'dev', 'xyz', 'link', 'site',
  'online', 'store', 'shop', 'club', 'live', 'mx', 'ar', 'cl', 'pe', 'br', 'ec', 'bo', 'py', 'gt', 'cr',
];
// The host must start the message or follow whitespace/opening punctuation, not a word glued by a period.
const BARE_DOMAIN = new RegExp(
  String.raw`(?:^|[\s(<"'¿¡])[a-z0-9-]+(?:\.[a-z0-9-]+)*\.(?:${BARE_DOMAIN_TLDS.join('|')})(?![a-z0-9-])`,
  'i',
);

// Emails, matched on normalized (lowercase, accent-free) text. The local part must start a token (the
// lookbehind keeps the scan linear) and the domain must look like a host with a TLD, so "@juan",
// "precio @ 200" or "2x1 @ tienda" never match. Every quantifier is on a class disjoint from what follows.
const EMAIL_LOCAL = String.raw`(?<![a-z0-9._%+-])[a-z0-9._%+-]+`;
const EMAIL_HOST = String.raw`[a-z0-9-]+(?:\.[a-z0-9-]+)*`;
// Plain "x@y.tld": the glued shape is unambiguous, so any alphabetic TLD counts ("juan@empresa.es").
const EMAIL = new RegExp(String.raw`${EMAIL_LOCAL}@${EMAIL_HOST}\.[a-z]{2,}(?![a-z0-9-])`);
// Spaced/obfuscated "x @ y.tld", "x (at) y.tld", "x [at] y.tld", "x arroba y.tld": only on the curated
// TLD list, so "nos vemos @ casa.Me gusta" is not an email.
const OBFUSCATED_EMAIL = new RegExp(
  String.raw`${EMAIL_LOCAL}(?:\s{0,3}(?:@|\(at\)|\[at\])\s{0,3}|\s{1,3}arroba\s{1,3})` +
    String.raw`${EMAIL_HOST}\.(?:${BARE_DOMAIN_TLDS.join('|')})(?![a-z0-9-])`,
);

function hasEmail(content) {
  if (!/[@(\[]|arroba/i.test(content)) return false; // cheap pre-check, no normalization needed
  const text = normalize(content);
  return EMAIL.test(text) || OBFUSCATED_EMAIL.test(text);
}

/** Lowercase and strip accents/diacritics: "TÓNTÖ" -> "tonto". */
export function normalize(text) {
  return text.normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase();
}

/** Comma-separated list -> normalized, de-duplicated, non-empty words/phrases. */
export function parseBannedWords(csv) {
  return mergeBannedWords(String(csv ?? '').split(','));
}

/** Normalizes and de-duplicates banned words/phrases from any number of lists. */
export function mergeBannedWords(...lists) {
  const words = new Set();
  for (const list of lists) {
    for (const raw of list ?? []) {
      const word = toWords(normalize(String(raw)));
      if (word) words.add(word);
    }
  }
  return [...words];
}

function toWords(normalized) {
  return normalized.replace(/[^\p{L}\p{N}]+/gu, ' ').trim();
}

function hasBannedWord(content, bannedWords) {
  if (bannedWords.length === 0) return false;
  // Whole-word / whole-phrase match on the normalized text, so "tonto" does not hit "tontomovil".
  const padded = ` ${toWords(normalize(content))} `;
  return bannedWords.some((word) => padded.includes(` ${word} `));
}

// Telegram / WhatsApp short links, blocked explicitly because ".me" is not on the bare-domain list.
// Same boundary rule as bare domains, so "quiero.me" or "ok.me" are not matched.
const SHORT_LINK = /(?:^|[\s(<"'¿¡])(?:t|wa)\.me(?![a-z0-9-])/i;

function hasLink(content) {
  return EXPLICIT_LINK.test(content) || BARE_DOMAIN.test(content) || SHORT_LINK.test(content);
}

/**
 * Linear scan over digit groups. Adjacent groups join into one number only when the run looks like a
 * phone: it starts with "+" (country code), the separator holds a parenthesis ("(55) 1234"), or one of
 * the two groups has 3+ digits. Chains of 1-2 digit numbers ("tallas 36 38 40", "36-38-40") never join.
 */
function hasPhoneNumber(content) {
  let digits = 0;
  let prevEnd = -1;
  let prevLength = 0;
  let international = false;

  for (const match of content.matchAll(/\d+/g)) {
    const length = match[0].length;
    const separator = prevEnd < 0 ? null : content.slice(prevEnd, match.index);
    if (separator === null || !joins(separator, prevLength, length, international)) {
      digits = 0;
      international = /\+\(?$/.test(content.slice(Math.max(0, match.index - 2), match.index));
    }
    digits += length;
    if (digits >= PHONE_MIN_DIGITS) return true;
    prevEnd = match.index + length;
    prevLength = length;
  }
  return false;
}

function joins(separator, previousLength, length, international) {
  if (!/^[ .()-]{1,3}$/.test(separator) || separator.includes('  ')) return false;
  return international || separator.includes(')') || previousLength >= 3 || length >= 3;
}

/**
 * @param {string} content message text
 * @param {string[]} bannedWords already normalized (see mergeBannedWords)
 * @returns {{result: 'ALLOW'} | {result: 'DENY', reason: 'EMPTY'|'BANNED_WORD'|'EMAIL'|'LINK'|'PHONE'}}
 */
export function reviewContent(content, bannedWords) {
  if (content.trim().length === 0) return { result: 'DENY', reason: 'EMPTY' };
  if (hasBannedWord(content, bannedWords)) return { result: 'DENY', reason: 'BANNED_WORD' };
  // Before LINK: an email's domain ("juan @ gmail.com") would otherwise be reported as a bare-domain link.
  if (hasEmail(content)) return { result: 'DENY', reason: 'EMAIL' };
  if (hasLink(content)) return { result: 'DENY', reason: 'LINK' };
  if (hasPhoneNumber(content)) return { result: 'DENY', reason: 'PHONE' };
  return { result: 'ALLOW' };
}

const defaultLogger = {
  info: (entry) => console.log(JSON.stringify(entry)),
  warn: (entry) => console.warn(JSON.stringify(entry)),
};

/**
 * Builds the IVS Chat message review handler. Only ids are ever logged: never content, IP or names.
 *
 * @param {object} deps
 * @param {string[]} deps.bannedWords banned words/phrases (normalized here)
 * @param {(body: string, signal: AbortSignal) => Promise<unknown>} deps.sendToQueue sends one SQS message
 * @param {number} [deps.sendTimeoutMs] hard cap on the SQS send; IVS waits at most 200 ms for the review
 * @param {() => Date} [deps.now]
 * @param {{info: Function, warn: Function}} [deps.logger]
 */
export function createHandler({ bannedWords, sendToQueue, sendTimeoutMs = 100, now = () => new Date(), logger = defaultLogger }) {
  const normalizedBannedWords = mergeBannedWords(bannedWords);

  async function enqueue(event, content) {
    const ids = { messageId: event.MessageId, roomArn: event.RoomArn };
    const sender = event.Sender ?? {};
    const liveId = sender.Attributes?.liveId;
    if (!liveId) {
      logger.warn({ event: 'enqueue_failed', reason: 'missing_live_id', ...ids });
      return;
    }

    const body = JSON.stringify({
      messageId: event.MessageId,
      liveId,
      userId: sender.UserId,
      displayName: sender.Attributes?.displayName,
      content,
      sentAt: now().toISOString(),
    });

    const controller = new AbortController();
    let timer;
    const timedOut = new Promise((resolve) => {
      timer = setTimeout(() => {
        controller.abort();
        resolve('timeout');
      }, sendTimeoutMs);
    });

    try {
      const send = Promise.resolve().then(() => sendToQueue(body, controller.signal)).then(() => 'sent');
      if ((await Promise.race([send, timedOut])) === 'timeout') {
        logger.warn({ event: 'enqueue_failed', reason: 'timeout', ...ids, timeoutMs: sendTimeoutMs });
      }
    } catch (err) {
      // Never change the review result because persistence failed.
      logger.warn({ event: 'enqueue_failed', reason: 'error', ...ids, error: err?.name ?? 'Error' });
    } finally {
      clearTimeout(timer);
    }
  }

  return async function handler(event) {
    const content = typeof event?.Content === 'string' ? event.Content : '';
    const review = reviewContent(content, normalizedBannedWords);

    if (review.result === 'DENY') {
      logger.info({ event: 'message_denied', reason: review.reason, messageId: event?.MessageId, roomArn: event?.RoomArn });
      return {
        ReviewResult: 'DENY',
        Content: content.length > 0 ? content : ' ', // response contract requires Content length >= 1
        Attributes: { Reason: review.reason },
      };
    }

    await enqueue(event, content);
    return { ReviewResult: 'ALLOW', Content: content, Attributes: event.Attributes ?? {} };
  };
}
