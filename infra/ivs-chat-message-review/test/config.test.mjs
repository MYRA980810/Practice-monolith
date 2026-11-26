import { test } from 'node:test';
import assert from 'node:assert/strict';
import { loadConfig } from '../src/config.mjs';

const quiet = { warn() {} };

test('merges banned words from BANNED_WORDS and the packaged JSON file', () => {
  const config = loadConfig({
    env: { BANNED_WORDS: 'Tonto, Estafa Total', QUEUE_URL: 'https://sqs.us-east-1.amazonaws.com/1/ivs-chat-messages' },
    readBannedWordsFile: () => '["IDIOTA", "tonto"]',
    logger: quiet,
  });

  assert.deepEqual(config.bannedWords.sort(), ['estafa total', 'idiota', 'tonto']);
  assert.equal(config.queueUrl, 'https://sqs.us-east-1.amazonaws.com/1/ivs-chat-messages');
});

test('an unreadable or invalid banned-words file falls back to BANNED_WORDS only', () => {
  for (const read of [() => '{oops', () => '{"not":"an array"}', () => { throw new Error('ENOENT'); }]) {
    const config = loadConfig({ env: { BANNED_WORDS: 'tonto' }, readBannedWordsFile: read, logger: quiet });
    assert.deepEqual(config.bannedWords, ['tonto']);
  }
});

test('SQS send timeout defaults to 800 ms and is capped below the 1 s Lambda timeout', () => {
  const timeout = (value) => loadConfig({
    env: value === undefined ? {} : { SQS_SEND_TIMEOUT_MS: value },
    readBannedWordsFile: () => '[]',
    logger: quiet,
  }).sendTimeoutMs;

  assert.equal(timeout(undefined), 800);
  assert.equal(timeout('abc'), 800);
  assert.equal(timeout('0'), 800);
  assert.equal(timeout('-5'), 800);
  assert.equal(timeout('80'), 80);
  assert.equal(timeout('900'), 900);
  assert.equal(timeout('5000'), 900);
});
