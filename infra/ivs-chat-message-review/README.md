# ivs-chat-message-review

Amazon IVS Chat **message review handler** (Lambda, Node.js 20, ESM). IVS Chat calls it for every
message sent to a room created with `messageReviewHandler` (the backend sets it when
`IVS_CHAT_REVIEW_HANDLER_ARN` is configured, see `IvsChatRoomAdapter`).

It does two things:

1. **Reviews** the message with stateless checks only (IVS gives the whole review 200 ms):
   - empty / whitespace-only → `DENY` (`Reason: EMPTY`)
   - banned word or phrase, matched as whole words after lowercasing and stripping accents → `DENY` (`BANNED_WORD`)
   - links → `DENY` (`LINK`): anything with `http(s)://` or `www.`, plus bare domains
     (`mitienda.com`, `bit.ly/x`) on a curated TLD list that leaves out TLDs that are also short
     Spanish words (`me`, `es`, `do`, `co`, ...), so "ok.me interesa" is not a link; the
     Telegram/WhatsApp short links `t.me` and `wa.me` are blocked explicitly
   - phone numbers → `DENY` (`PHONE`): 10+ digits in a phone-shaped run (`5512345678`,
     `55 1234 5678`, `+52 55 1234 5678`, `(55) 1234-5678`); chains of 1–2 digit numbers
     ("tallas 36 38 40 42 44 46") never join
   - anything else → `ALLOW`. Length and rate are enforced by the room limits.
2. **Hands allowed messages to SQS** (`ivs-chat-messages`) so the backend persists the chat
   history (`IvsChatMessageListener`). The send is capped at `SQS_SEND_TIMEOUT_MS` and never
   changes the result: if SQS fails or is slow, the message is still `ALLOW`ed (just not persisted).

The room's `fallbackResult` is `ALLOW`: if the Lambda errors or exceeds 200 ms, IVS delivers the
message anyway. Message content, IPs and names are never logged, only message ids and room ARNs.

## Contract

Input (from IVS Chat):

```json
{
  "Content": "string",
  "MessageId": "string",
  "RoomArn": "string",
  "Attributes": { "string": "string" },
  "Sender": { "UserId": "string", "Ip": "string", "Attributes": { "liveId": "...", "displayName": "...", "role": "..." } }
}
```

`Sender.Attributes` are the chat-token attributes the backend sets (`liveId`, `displayName`, `role`).

Output:

```json
{ "ReviewResult": "ALLOW" | "DENY", "Content": "string", "Attributes": { "string": "string" } }
```

On `ALLOW` the original `Content` and `Attributes` are returned unchanged. On `DENY`,
`Attributes.Reason` is one of `EMPTY`, `BANNED_WORD`, `LINK`, `PHONE`; the sender gets it in the
WebSocket 406 error.

SQS message body (on `ALLOW` only, and only when `Sender.Attributes.liveId` is present):

```json
{
  "messageId": "IVS message id",
  "liveId": "UUID",
  "userId": "UUID (Sender.UserId)",
  "displayName": "string",
  "content": "string",
  "sentAt": "ISO-8601 UTC, review time"
}
```

## Configuration

| Env var | Required | Description |
| --- | --- | --- |
| `QUEUE_URL` | yes | URL of the `ivs-chat-messages` queue. Without it nothing is persisted (still `ALLOW`). |
| `BANNED_WORDS` | no | Comma-separated words/phrases, merged with `banned-words.json`. |
| `SQS_SEND_TIMEOUT_MS` | no | Cap on the SQS send. Default `100`, max `150`. |

`banned-words.json` is a packaged starter list (JSON array of strings); review it with the
business before going live. Changing it requires a redeploy; `BANNED_WORDS` only a config update.

## Development

```bash
npm test            # node:test, no AWS access needed (src/review.mjs and src/config.mjs are SDK-free)
npm run package     # function.zip (zip CLI); on Windows: npm run package:windows
```

This folder is not part of the Maven build or the Docker image (the Dockerfile only copies `src/`).

## Deploy (AWS CLI)

Set these once in your shell (examples use bash):

```bash
export AWS_REGION=us-east-1
export ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
export QUEUE=ivs-chat-messages
export FUNCTION=ivs-chat-message-review
```

### 1. Queue + DLQ

```bash
aws sqs create-queue --queue-name ${QUEUE}-dlq \
  --attributes MessageRetentionPeriod=1209600

export DLQ_ARN=arn:aws:sqs:${AWS_REGION}:${ACCOUNT_ID}:${QUEUE}-dlq

aws sqs create-queue --queue-name ${QUEUE} \
  --attributes '{
    "VisibilityTimeout": "60",
    "MessageRetentionPeriod": "345600",
    "RedrivePolicy": "{\"deadLetterTargetArn\":\"'"${DLQ_ARN}"'\",\"maxReceiveCount\":\"15\"}"
  }'

export QUEUE_URL=$(aws sqs get-queue-url --queue-name ${QUEUE} --query QueueUrl --output text)
export QUEUE_ARN=arn:aws:sqs:${AWS_REGION}:${ACCOUNT_ID}:${QUEUE}
```

The backend lets every processing failure propagate, so a failing message is retried every 60 s
(visibility timeout) and moves to the DLQ after 15 receives: a ~15 min database outage is ridden
out without flooding the DLQ. The trade-off is that a poison message (malformed payload, unknown
live) also takes ~15 min to reach the DLQ, and retention must outlast that retry window: keep the
main queue at the default 4 days and the DLQ at 14 days. Processing one message is a single insert,
far below the 60 s visibility timeout. After fixing the cause, redrive with
`aws sqs start-message-move-task --source-arn $DLQ_ARN` (alarms: step 7).

### 2. Lambda execution role

```bash
aws iam create-role --role-name ${FUNCTION}-role \
  --assume-role-policy-document '{
    "Version": "2012-10-17",
    "Statement": [{ "Effect": "Allow", "Principal": { "Service": "lambda.amazonaws.com" }, "Action": "sts:AssumeRole" }]
  }'

aws iam attach-role-policy --role-name ${FUNCTION}-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

aws iam put-role-policy --role-name ${FUNCTION}-role --policy-name send-chat-messages \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [{ "Effect": "Allow", "Action": "sqs:SendMessage", "Resource": "'"${QUEUE_ARN}"'" }]
  }'
```

### 3. Function

```bash
npm run package

aws lambda create-function --function-name ${FUNCTION} \
  --runtime nodejs20.x --architectures arm64 \
  --handler src/index.handler \
  --role arn:aws:iam::${ACCOUNT_ID}:role/${FUNCTION}-role \
  --memory-size 256 --timeout 1 \
  --zip-file fileb://function.zip \
  --environment '{"Variables":{"QUEUE_URL":"'"${QUEUE_URL}"'","SQS_SEND_TIMEOUT_MS":"100"}}'

export FUNCTION_ARN=$(aws lambda get-function --function-name ${FUNCTION} --query Configuration.FunctionArn --output text)
```

Updates:

```bash
npm run package
aws lambda update-function-code --function-name ${FUNCTION} --zip-file fileb://function.zip
aws lambda update-function-configuration --function-name ${FUNCTION} \
  --environment '{"Variables":{"QUEUE_URL":"'"${QUEUE_URL}"'","SQS_SEND_TIMEOUT_MS":"100","BANNED_WORDS":"palabra1,palabra2"}}'
```

Always use the JSON form: the `Variables={K=V,...}` shorthand splits on commas, which breaks
`BANNED_WORDS`. `update-function-configuration --environment` **replaces all** environment
variables, so pass the full set every time (`QUEUE_URL` included), not only the one you change.

### 4. Allow IVS Chat to invoke it

```bash
aws lambda add-permission --function-name ${FUNCTION} \
  --statement-id ivschat-message-review \
  --action lambda:InvokeFunction \
  --principal ivschat.amazonaws.com \
  --source-account ${ACCOUNT_ID} \
  --source-arn "arn:aws:ivschat:${AWS_REGION}:${ACCOUNT_ID}:room/*"
```

### 5. Backend IAM user (static keys used by Railway)

Attach to the backend's IAM user (`<backend-user>`), in addition to what it already has:

```bash
aws iam put-user-policy --user-name <backend-user> --policy-name ivs-chat \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "ivschat:CreateRoom", "ivschat:DeleteRoom", "ivschat:CreateChatToken",
          "ivschat:SendEvent", "ivschat:DeleteMessage", "ivschat:DisconnectUser"
        ],
        "Resource": "arn:aws:ivschat:'"${AWS_REGION}"':'"${ACCOUNT_ID}"':room/*"
      },
      {
        "Effect": "Allow",
        "Action": ["ivschat:TagResource"],
        "Resource": "arn:aws:ivschat:'"${AWS_REGION}"':'"${ACCOUNT_ID}"':room/*"
      },
      {
        "Effect": "Allow",
        "Action": ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes", "sqs:GetQueueUrl"],
        "Resource": "'"${QUEUE_ARN}"'"
      }
    ]
  }'
```

`ivschat:TagResource` covers tag-on-create (rooms are created with `liveId`/`sellerId` tags); drop
it if your existing policy already lets `CreateRoom` succeed with tags. Creating a
room with a review handler does not need `lambda:*` on the backend user; the resource policy from
step 4 is what lets IVS call the function.

### 6. Railway

Set on the backend service, **in this order**:

1. `IVS_CHAT_MESSAGES_QUEUE_NAME=ivs-chat-messages` (also the default).
2. `IVS_CHAT_REVIEW_HANDLER_ARN=${FUNCTION_ARN}`.
3. Only once the queue exists: `LIVE_CHAT_PROVIDER=ivs`.

The SQS listener is registered only when `live.chat-provider=ivs`, and
`spring.cloud.aws.sqs.queue-not-found-strategy` is `fail`: flipping the provider before the queue
exists stops the backend from starting.

The review handler is attached when a room is created, so only lives started after step 2 are
moderated and persisted.

### 7. Alarms

```bash
aws sns create-topic --name ivs-chat-alerts
export TOPIC_ARN=arn:aws:sns:${AWS_REGION}:${ACCOUNT_ID}:ivs-chat-alerts
aws sns subscribe --topic-arn ${TOPIC_ARN} --protocol email --notification-endpoint <alerts-email>
# Confirm the subscription from the email AWS sends.

# Any message in the DLQ = chat history the backend could not persist.
aws cloudwatch put-metric-alarm --alarm-name ${QUEUE}-dlq-not-empty \
  --namespace AWS/SQS --metric-name ApproximateNumberOfMessagesVisible \
  --dimensions Name=QueueName,Value=${QUEUE}-dlq \
  --statistic Maximum --period 300 --evaluation-periods 1 \
  --threshold 0 --comparison-operator GreaterThanThreshold \
  --treat-missing-data notBreaching \
  --alarm-actions ${TOPIC_ARN}

# Allowed messages the Lambda could not hand to SQS (never persisted). The Lambda logs one JSON line
# per failure with the fixed token "event":"enqueue_failed" and "reason": timeout | error | missing_live_id.
aws logs create-log-group --log-group-name /aws/lambda/${FUNCTION}   # skip if it already exists

aws logs put-metric-filter --log-group-name /aws/lambda/${FUNCTION} \
  --filter-name enqueue-failed \
  --filter-pattern '"enqueue_failed"' \
  --metric-transformations metricName=ChatEnqueueFailed,metricNamespace=Livecomerce/IvsChat,metricValue=1,defaultValue=0

aws cloudwatch put-metric-alarm --alarm-name ${FUNCTION}-enqueue-failed \
  --namespace Livecomerce/IvsChat --metric-name ChatEnqueueFailed \
  --statistic Sum --period 300 --evaluation-periods 1 \
  --threshold 5 --comparison-operator GreaterThanOrEqualToThreshold \
  --treat-missing-data notBreaching \
  --alarm-actions ${TOPIC_ARN}
```

The filter uses a plain term match, so it works with both the text and JSON Lambda log formats.
Denied messages log `"event":"message_denied"` with the `reason`, if you also want a moderation metric.

## Go-live checklist

1. Steps 1–5 and 7 done, `npm test` green, function deployed.
2. Railway, in order: `IVS_CHAT_MESSAGES_QUEUE_NAME`, then `IVS_CHAT_REVIEW_HANDLER_ARN`, then
   `LIVE_CHAT_PROVIDER=ivs` **only after the queue exists** (otherwise the backend fails to start).
3. Start a test live (its room is created with the review handler) and send a few messages.
4. Verify the message id contract: the `messageId` persisted in `live_chat_messages` (column
   `agora_msg_id`) must equal the `Id` the client receives for the same message, and a seller delete
   (`DeleteMessage`) with that id must soft-delete the persisted row. AWS docs only say the review
   handler's `MessageId` is "generated by IVS Chat"; if the ids differ, moderation deletes will not
   hide messages from the history.
5. Send a denied message (link, phone, banned word): the sender gets the 406 reason and nothing is persisted.

## Known limitations

- Messages allowed by the room's `fallbackResult` (Lambda error, cold start or > 200 ms) are
  delivered but not persisted to the history. Keep the function warm or add provisioned
  concurrency if that gap matters.
- `sentAt` is the review time, not a client timestamp (IVS does not pass one to the handler).

### Known limitations of the heuristics

- Phones written only as 2-digit pairs (`55-12-34-56-78`, `55 12 34 56 78`) are not detected:
  they are indistinguishable from size or quantity lists (`36-38-40-42-44`).
- Sequences of 3+ digit groups such as order numbers (`pedido 123 456 789 012`) can be denied as
  `PHONE`. Phone detection also needs 10+ digits, so shorter local numbers pass.
- `banned-words.json` is a starter list; the business must review and extend it before go-live.
  Matching is whole-word on normalized text, so leetspeak and spacing tricks ("t o n t o") pass.
- Bare domains on the excluded TLDs (`tienda.me`, `ejemplo.es`) pass unless written with
  `http(s)://` or `www.`; `t.me` and `wa.me` are the only `.me` hosts blocked without them.
