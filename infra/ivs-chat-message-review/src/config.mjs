import { mergeBannedWords, parseBannedWords } from './review.mjs';

// IVS Chat waits at most 200 ms and then falls back to ALLOW; the send keeps running past that so the
// message still reaches the history (e.g. on a cold start). The cap only has to stay below the 1 s
// Lambda function timeout.
const DEFAULT_SEND_TIMEOUT_MS = 800;
const MAX_SEND_TIMEOUT_MS = 900;

/**
 * Reads the Lambda configuration once, at cold start.
 *
 * - QUEUE_URL: URL of the ivs-chat-messages SQS queue.
 * - BANNED_WORDS: optional comma-separated words/phrases, merged with banned-words.json.
 * - SQS_SEND_TIMEOUT_MS: optional cap on the SQS send (default 800, max 900).
 */
export function loadConfig({ env, readBannedWordsFile, logger }) {
  return {
    queueUrl: env.QUEUE_URL,
    sendTimeoutMs: sendTimeout(env.SQS_SEND_TIMEOUT_MS),
    bannedWords: mergeBannedWords(parseBannedWords(env.BANNED_WORDS), fileBannedWords(readBannedWordsFile, logger)),
  };
}

function sendTimeout(raw) {
  const value = Number.parseInt(raw ?? '', 10);
  if (!Number.isFinite(value) || value <= 0) return DEFAULT_SEND_TIMEOUT_MS;
  return Math.min(value, MAX_SEND_TIMEOUT_MS);
}

function fileBannedWords(readBannedWordsFile, logger) {
  try {
    const words = JSON.parse(readBannedWordsFile());
    if (Array.isArray(words)) return words;
    logger.warn({ msg: 'banned-words.json is not a JSON array, ignored' });
  } catch (err) {
    logger.warn({ msg: 'banned-words.json unreadable, ignored', error: err?.name ?? 'Error' });
  }
  return [];
}
