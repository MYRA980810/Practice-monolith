// Lambda entry point: wires the review handler to the real SQS client and configuration.
import { readFileSync } from 'node:fs';
import { SQSClient, SendMessageCommand } from '@aws-sdk/client-sqs';
import { createHandler } from './review.mjs';
import { loadConfig } from './config.mjs';

const logger = {
  info: (entry) => console.log(JSON.stringify(entry)),
  warn: (entry) => console.warn(JSON.stringify(entry)),
};

const config = loadConfig({
  env: process.env,
  readBannedWordsFile: () => readFileSync(new URL('../banned-words.json', import.meta.url), 'utf8'),
  logger,
});

// Created once per execution environment and reused across invocations.
// A single attempt: the send is capped by SQS_SEND_TIMEOUT_MS, below the 1 s function timeout.
const sqs = new SQSClient({ maxAttempts: 1 });

async function sendToQueue(body, abortSignal) {
  if (!config.queueUrl) throw new Error('QUEUE_URL is not set');
  await sqs.send(new SendMessageCommand({ QueueUrl: config.queueUrl, MessageBody: body }), { abortSignal });
}

export const handler = createHandler({
  bannedWords: config.bannedWords,
  sendToQueue,
  sendTimeoutMs: config.sendTimeoutMs,
  logger,
});
