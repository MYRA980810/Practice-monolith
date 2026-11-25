import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createHandler, reviewContent, parseBannedWords } from '../src/review.mjs';

const LIVE_ID = '6f1c2a8e-3b0d-4c55-9a1e-2f7d8b9c0a11';
const USER_ID = '0b8f6c1d-7e2a-4d39-8c5b-1a2b3c4d5e6f';
const NOW = new Date('2026-11-22T15:04:05.123Z');

function event(content, overrides = {}) {
  return {
    Content: content,
    MessageId: 'Ab1cDeFgHiJk',
    RoomArn: 'arn:aws:ivschat:us-east-1:123456789012:room/abc',
    Attributes: { kind: 'text' },
    Sender: {
      UserId: USER_ID,
      Ip: '203.0.113.10',
      Attributes: { liveId: LIVE_ID, displayName: 'Alice', role: 'BUYER' },
    },
    ...overrides,
  };
}

const silentLogger = { info() {}, warn() {} };

function setup({ send = async () => {}, bannedWords = ['tonto', 'estafa total'], timeoutMs = 50 } = {}) {
  const sent = [];
  const handler = createHandler({
    bannedWords,
    sendTimeoutMs: timeoutMs,
    now: () => NOW,
    logger: silentLogger,
    sendToQueue: async (body, signal) => {
      sent.push(JSON.parse(body));
      return send(body, signal);
    },
  });
  return { handler, sent };
}

test('ALLOW: returns the original content and attributes and enqueues the message', async () => {
  const { handler, sent } = setup();

  const result = await handler(event('Hola, me encanta este producto'));

  assert.deepEqual(result, {
    ReviewResult: 'ALLOW',
    Content: 'Hola, me encanta este producto',
    Attributes: { kind: 'text' },
  });
  assert.deepEqual(sent, [{
    messageId: 'Ab1cDeFgHiJk',
    liveId: LIVE_ID,
    userId: USER_ID,
    displayName: 'Alice',
    content: 'Hola, me encanta este producto',
    sentAt: '2026-11-22T15:04:05.123Z',
  }]);
});

test('DENY: empty and whitespace-only messages', async () => {
  const { handler, sent } = setup();

  for (const content of ['', '   ', '\n\t ']) {
    const result = await handler(event(content));
    assert.equal(result.ReviewResult, 'DENY');
    assert.equal(result.Attributes.Reason, 'EMPTY');
    assert.ok(result.Content.length >= 1, 'Content must satisfy the min length of the response contract');
  }
  assert.equal(sent.length, 0);
});

test('DENY: banned word regardless of case and accents', async () => {
  const { handler, sent } = setup({ bannedWords: ['tonto', 'estafa total', 'pendéjo'] });

  for (const content of ['Eres un TONTO', 'eres un tóntö!', 'esto es una ESTAFA   total', 'qué PENDEJO']) {
    const result = await handler(event(content));
    assert.equal(result.ReviewResult, 'DENY', content);
    assert.equal(result.Attributes.Reason, 'BANNED_WORD');
  }
  assert.equal(sent.length, 0);
});

test('ALLOW: banned word only as part of a longer word', async () => {
  const { handler } = setup({ bannedWords: ['tonto'] });

  assert.equal((await handler(event('el tontomóvil'))).ReviewResult, 'ALLOW');
});

test('DENY: links', async () => {
  const { handler, sent } = setup();

  for (const content of [
    'mira https://example.com/oferta',
    'https://x.com',
    'entra a http://bit.ly/x',
    'en www.tienda-falsa.mx',
    'www.tienda.com',
    'búscame en ofertas.com',
    'visita mitienda.com',
    'tienda.com.mx',
    'bit.ly/abc',
    'Compra en MiTienda.Store/promo',
    'https://tienda.me/oferta',
    'www.ejemplo.es',
  ]) {
    const result = await handler(event(content));
    assert.equal(result.ReviewResult, 'DENY', content);
    assert.equal(result.Attributes.Reason, 'LINK');
  }
  assert.equal(sent.length, 0);
});

test('DENY: Telegram and WhatsApp short links even though .me is not a generic TLD', async () => {
  const { handler, sent } = setup();

  for (const content of ['t.me/canal', 'T.ME/x', 'wa.me/5215512345678', 'únete en t.me/tienda', '(wa.me)', 't.me']) {
    const result = await handler(event(content));
    assert.equal(result.ReviewResult, 'DENY', content);
    assert.equal(result.Attributes.Reason, 'LINK', content);
  }
  assert.equal(sent.length, 0);
});

test('ALLOW: words ending in t.me / wa.me glued to a word are not short links', async () => {
  const { handler } = setup();

  for (const content of ['ok.me interesa', 'quiero.me', 'gusta.me mucho', 'nota.mensaje']) {
    assert.equal((await handler(event(content))).ReviewResult, 'ALLOW', content);
  }
});

test('ALLOW: words glued by a period are not links', async () => {
  const { handler } = setup();

  for (const content of [
    'ok.me interesa',
    'si.es de algodon',
    'lo quiero.do',
    'Quiero la roja.Me la envias',
    'gracias.Ya lo pedí',
    'bonito.Se ve bien',
    'claro.no hay problema',
    'uy.que bonito',
  ]) {
    assert.equal((await handler(event(content))).ReviewResult, 'ALLOW', content);
  }
});

test('DENY: email addresses are reported as EMAIL, not LINK', async () => {
  const { handler, sent } = setup();

  for (const content of [
    'juan@gmail.com',
    'escríbeme a Juan.Perez+x@empresa.com.mx porfa',
    'JUAN@GMAIL.COM',
    'mi correo: (juan_99@hotmail.com).',
    'correo<ventas@tienda-mx.net>',
    'josé@gmail.com',
    'juan@empresa.es',
    'juan @ gmail.com',
    'juan@ gmail.com',
    'juan  @  outlook.com.mx',
    'juan arroba gmail.com',
    'juan ARROBA yahoo.com.mx',
    'juan(at)gmail.com',
    'juan [at] gmail.com',
  ]) {
    const result = await handler(event(content));
    assert.equal(result.ReviewResult, 'DENY', content);
    assert.equal(result.Attributes.Reason, 'EMAIL', content);
  }
  assert.equal(sent.length, 0);
});

test('ALLOW: @ mentions and @ without a domain-shaped part are not emails', async () => {
  const { handler } = setup();

  for (const content of [
    '@juan me encanta',
    'hola @maria',
    'precio @ 200',
    '2x1 @ tienda',
    'correo@gmail',
    'juan arroba gmail',
    'una arroba de maíz',
    'nos vemos @ casa.Me gusta',
    'mira el (at) de la foto',
  ]) {
    assert.equal((await handler(event(content))).ReviewResult, 'ALLOW', content);
  }
});

test('DENY: phone numbers', async () => {
  const { handler, sent } = setup();

  for (const content of [
    'llámame al 5512345678',
    '55 1234 5678',
    'whats +52 55 1234 5678',
    'tel (55) 1234-5678',
    '55-1234-5678',
    'marca 55.1234.5678',
    '+52 1 55 1234 5678',
  ]) {
    const result = await handler(event(content));
    assert.equal(result.ReviewResult, 'DENY', content);
    assert.equal(result.Attributes.Reason, 'PHONE');
  }
  assert.equal(sent.length, 0);
});

test('ALLOW: prices, quantities, sizes and short numbers are not phone numbers', async () => {
  const { handler } = setup();

  for (const content of [
    '¿cuesta $1,500?',
    'quiero 2 talla 38',
    'precio 1.250.000',
    'son 3 x 100',
    'tallas 36 38 40 42 44 46',
    'tallas 36-38-40-42-44-46',
    'compro 10 20 30 40 50',
    'cuesta $1,250.00',
    'talla 38 y 40',
    'pedido 12 34 56 78 90',
  ]) {
    assert.equal((await handler(event(content))).ReviewResult, 'ALLOW', content);
  }
});

test('review cost stays trivial on worst-case 500-char input', () => {
  const inputs = [
    '1 '.repeat(250),
    '1-'.repeat(250),
    '12.'.repeat(166),
    'a.'.repeat(250),
    'ab.'.repeat(166) + 'x',
    '+('.repeat(250),
    'a'.repeat(500),
    'a@'.repeat(250),
    'a.'.repeat(249) + '@',
    'a @ '.repeat(125),
    'a arroba '.repeat(55),
    'x@' + 'a.'.repeat(249),
  ];
  const started = performance.now();
  for (let i = 0; i < 100; i++) {
    for (const input of inputs) reviewContent(input, ['tonto']);
  }
  const perReview = (performance.now() - started) / (100 * inputs.length);
  assert.ok(perReview < 1, `average review took ${perReview.toFixed(3)} ms`);
});

test('SQS failure: still ALLOW', async () => {
  const { handler, sent } = setup({ send: async () => { throw new Error('AccessDenied'); } });

  const result = await handler(event('hola'));

  assert.equal(result.ReviewResult, 'ALLOW');
  assert.equal(sent.length, 1);
});

test('SQS timeout: still ALLOW, within the budget, and the send is aborted', async () => {
  let abortedSignal;
  const { handler } = setup({
    timeoutMs: 30,
    send: (_body, signal) => {
      abortedSignal = signal;
      return new Promise(() => {}); // never settles, like a hung network call
    },
  });

  const started = Date.now();
  const result = await handler(event('hola'));
  const elapsed = Date.now() - started;

  assert.equal(result.ReviewResult, 'ALLOW');
  assert.ok(elapsed < 150, `review took ${elapsed} ms`);
  assert.equal(abortedSignal.aborted, true);
});

test('ALLOW without liveId attribute: not enqueued, still ALLOW', async () => {
  const { handler, sent } = setup();

  const result = await handler(event('hola', {
    Sender: { UserId: USER_ID, Attributes: { displayName: 'Alice' } },
  }));

  assert.equal(result.ReviewResult, 'ALLOW');
  assert.equal(sent.length, 0);
});

function capture(sendToQueue, timeoutMs = 20) {
  const entries = [];
  const handler = createHandler({
    bannedWords: [],
    sendTimeoutMs: timeoutMs,
    now: () => NOW,
    logger: { info: (e) => entries.push(e), warn: (e) => entries.push(e) },
    sendToQueue,
  });
  return { handler, entries };
}

test('enqueue failures log a stable, filterable event token', async () => {
  const failing = capture(async () => { throw new Error('AccessDenied'); });
  await failing.handler(event('hola'));
  assert.deepEqual(failing.entries.map((e) => [e.event, e.reason]), [['enqueue_failed', 'error']]);

  const hanging = capture(() => new Promise(() => {}));
  await hanging.handler(event('hola'));
  assert.deepEqual(hanging.entries.map((e) => [e.event, e.reason]), [['enqueue_failed', 'timeout']]);

  const noLive = capture(async () => {});
  await noLive.handler(event('hola', { Sender: { UserId: USER_ID, Attributes: {} } }));
  assert.deepEqual(noLive.entries.map((e) => [e.event, e.reason]), [['enqueue_failed', 'missing_live_id']]);

  const denied = capture(async () => {});
  await denied.handler(event('   '));
  assert.deepEqual(denied.entries.map((e) => [e.event, e.reason]), [['message_denied', 'EMPTY']]);
});

test('message content never reaches the logs', async () => {
  const lines = [];
  const logger = { info: (...a) => lines.push(JSON.stringify(a)), warn: (...a) => lines.push(JSON.stringify(a)) };
  const handler = createHandler({
    bannedWords: ['tonto'],
    sendTimeoutMs: 20,
    now: () => NOW,
    logger,
    sendToQueue: async () => { throw new Error('boom'); },
  });

  await handler(event('secreto-allow'));
  await handler(event('tonto secreto-deny'));
  await handler(event('llama 5512345678'));
  await handler(event('secreto@gmail.com'));

  assert.ok(lines.length > 0);
  for (const line of lines) {
    assert.ok(!line.includes('secreto') && !line.includes('5512345678') && !line.includes('gmail'), line);
  }
});

test('reviewContent and parseBannedWords normalize their input', () => {
  assert.deepEqual(parseBannedWords(' Tonto, ,ESTÚPIDO ,'), ['tonto', 'estupido']);
  assert.deepEqual(reviewContent('hola', ['hola']), { result: 'DENY', reason: 'BANNED_WORD' });
  assert.deepEqual(reviewContent('hola', []), { result: 'ALLOW' });
});
