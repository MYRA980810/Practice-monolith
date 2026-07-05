# Live Module — Technical Debt & Future Work

## Concurrent Stock — Testcontainers Integration Test

The `BuyLiveProductService` uses an atomic SQL UPDATE to prevent overselling:
```sql
UPDATE live_products SET stock_sold = stock_sold + :qty
WHERE id = :id AND (is_hot = true OR stock_sold + :qty <= stock_allocated)
```
This is unit-tested with mocks. A Testcontainers integration test with concurrent buyers
hitting the same live product is needed to verify the atomic guarantee under real PostgreSQL.

**File**: `src/test/java/com/livecomerce/live/infrastructure/persistence/ConcurrentStockReservationIT.java`
**Requires**: `testcontainers-bom`, `postgresql` Testcontainer dependency

## Post-Live Catalog Import

Hot products created during a live (is_hot=true) are persisted in `live_products` only.
After the live ends, the seller may want to promote them to their catalog. Currently there
is no workflow for this. Consider adding:
- A POST `/api/lives/{id}/products/{productId}/promote` endpoint
- `PromoteHotProductToLiveUseCase` that creates a `Product` in the catalog module

## Redis STOMP Relay for Multi-Instance

`WebSocketConfig` uses `enableSimpleBroker("/topic")` which is in-memory and not shared
across application instances. For production horizontal scaling, replace with:
```java
config.enableStompBrokerRelay("/topic")
      .setRelayHost(relayHost)
      .setRelayPort(61613)
      .setClientLogin(login)
      .setClientPasscode(passcode);
```
Requires RabbitMQ or ActiveMQ with STOMP protocol enabled.

## Agora Webhook — Full Event Processing

`AgoraWebhookController` currently only validates the signature and logs the event.
Implement handlers for:
- `eventType=1` (cloud recording started) → update live status or notify seller
- `eventType=31/32` (uplink/downlink network quality) → store metrics
- `eventType=103` (cloud recording file inflight) → update recording URL

## Saved Payment Methods for Buyers

No `saved_cards` or `payment_methods` table exists. Buyers must re-enter card details
for each live purchase. Add Stripe Payment Method storage for recurring buyers.
