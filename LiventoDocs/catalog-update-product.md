# Feature: Product Update — PUT Endpoint

**Date:** 2026-05-14
**Module:** `catalog`
**Author:** Christian Marquez
**Status:** Completed

---

## Context

The `catalog` module had no mechanism for a seller to modify an existing product. Operations like correcting a price, updating a description, or fixing a SKU required deleting and recreating the product — which is not viable in production.

Additionally, the update flow introduces a security concern that did not exist in read-only operations: a seller must only be able to modify products that belong to their own store. Without an explicit ownership check, any authenticated `SELLER` could modify another seller's products by knowing the product UUID.

---

## Decision: PUT over PATCH

Two HTTP semantics were evaluated:

| | PUT | PATCH |
|---|---|---|
| Semantics | Full resource replacement | Partial update |
| Client contract | All fields required | Only changed fields |
| Backend complexity | Low | Medium-high |
| Form UX fit | Natural — form sends all fields | Requires field diffing |

**PUT was chosen for the MVP** based on the frontend integration model: Next.js will present a pre-filled form with all current product values. On submit, the form sends every field regardless of what changed. This maps directly to PUT semantics — no field diffing required on either side.

PATCH becomes relevant when partial updates are needed outside a form context (bulk API consumers, mobile clients with offline sync). That use case does not exist in the MVP scope.

**Recommendation for future iterations:** migrate to PATCH when external API consumers or mobile clients require it. The domain `update()` method already accepts individual fields, so the change would be isolated to the API and application layers.

---

## Changes

### 1. `Product.java` — domain method

Added a controlled mutation method following the same pattern as `deactivate()`. All field changes go through the entity — no direct field access from outside.

```java
public void update(String name, String description, BigDecimal basePrice, String currency, String sku) {
    this.name        = name;
    this.description = description;
    this.basePrice   = basePrice;
    this.currency    = currency;
    this.sku         = sku;
    this.updatedAt   = OffsetDateTime.now();
}
```

Fields intentionally excluded from updates:

| Field | Reason |
|---|---|
| `id` | Immutable identifier |
| `storeId` | A product cannot be transferred between stores |
| `active` | Managed via a dedicated `deactivate()` operation |
| `stock` | Managed via `addStock`, `reserveStock`, `releaseStock`, `sellStock` |
| `images` | Managed via `addImage` |
| `createdAt` | Audit field — immutable |
| `updatedAt` | Auto-managed by every mutating method |

---

### 2. `UpdateProductUseCase.java` — input port

```java
public interface UpdateProductUseCase {

    record UpdateProductCommand(
            UUID productId,
            UUID storeId,
            String name,
            String description,
            BigDecimal basePrice,
            String currency,
            String sku
    ) {}

    Product update(UpdateProductCommand command);
}
```

`storeId` is included in the command not as a user-supplied value, but as the verified identity of the authenticated seller. The service uses it exclusively for ownership validation.

---

### 3. `UpdateProductService.java` — application service

```java
@Override
public Product update(UpdateProductCommand command) {
    var product = loadProductPort.loadById(command.productId())
            .orElseThrow(() -> new ProductNotFoundException(command.productId()));

    if (!product.getStoreId().equals(command.storeId())) {
        throw new AccessDeniedException("Product does not belong to this store");
    }

    product.update(
            command.name(),
            command.description(),
            command.basePrice(),
            command.currency(),
            command.sku()
    );

    return saveProductPort.save(product);
}
```

No new output port was needed. `SaveProductPort` already handles both create and update operations — the JPA `save()` method resolves the correct SQL operation (`INSERT` vs `UPDATE`) based on the entity's `isNew()` flag, which is managed by `Persistable<UUID>`.

---

### 4. `UpdateProductRequest.java` — API DTO

```java
record UpdateProductRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal basePrice,
        @NotBlank @Size(max = 3) String currency,
        @Size(max = 100) String sku
) {}
```

`currency` is `@NotBlank` here (unlike `CreateProductRequest` where it is optional). Since PUT sends a complete state, the currency must always be explicit — there is no "use the default" scenario when replacing a full resource.

---

### 5. `ProductController.java` — endpoint

```java
@PutMapping("/{id}")
@PreAuthorize("hasRole('SELLER')")
ResponseEntity<ProductResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateProductRequest request,
        @AuthenticationPrincipal UserPrincipal principal) {

    var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
    var product = updateProductUseCase.update(new UpdateProductUseCase.UpdateProductCommand(
            id,
            storeId,
            request.name(),
            request.description(),
            request.basePrice(),
            request.currency(),
            request.sku()
    ));
    return ResponseEntity.ok(ProductResponse.from(product));
}
```

The controller resolves `storeId` from the authenticated principal before constructing the command. The client never supplies the store — it is always derived from the token.

---

## Problems Encountered

### Problem 1 — `ProductNotFoundException` constructor mismatch

**Error**
```
The constructor ProductNotFoundException(String) is undefined
```

**Cause**

`ProductNotFoundException` was written to accept a `UUID`, not a `String`. The initial service implementation passed `command.productId().toString()`.

**Resolution**

Changed the argument to pass the `UUID` directly: `new ProductNotFoundException(command.productId())`.

---

## Security Model

The ownership check sits in the application layer, not the controller. This is intentional: the controller's responsibility is to resolve identity from the HTTP context and build the command. Enforcement of business rules — including access control over domain objects — belongs in the use case.

```
Request
  → Controller: resolves storeId from principal
  → Service: verifies product.storeId == command.storeId
  → Domain: mutates product fields
  → Persistence: saves updated state
```

If the product does not exist: `404 Not Found` via `ProductNotFoundException`.
If the product belongs to a different store: `403 Forbidden` via Spring Security's `AccessDeniedException`.

---

## Recommendations

### Short term
- Add a test to `UpdateProductService` covering the ownership check path. A seller attempting to update a foreign product must receive `403`, not `404` — leaking the existence of a resource is an information disclosure risk.

### Medium term (post-MVP)
- If external API consumers or mobile clients need partial updates, introduce `PATCH /api/products/{id}` as a parallel endpoint. The domain `update()` method is already suitable; the change would be limited to a new request DTO with `Optional<T>` fields and a new use case variant.

### Long term
- Consider event sourcing for price changes. Knowing the history of price modifications is valuable for analytics, disputes, and buyer trust. A `ProductPriceChangedEvent` published from `UpdateProductService` when `basePrice` differs from the loaded value would be a non-breaking addition.

---

## Files Changed

| File | Change |
|---|---|
| `catalog/domain/Product.java` | Added `update()` domain method |
| `catalog/application/port/in/UpdateProductUseCase.java` | Created — input port with `UpdateProductCommand` |
| `catalog/application/UpdateProductService.java` | Created — ownership check + domain mutation + persistence |
| `catalog/api/UpdateProductRequest.java` | Created — validated DTO for PUT body |
| `catalog/api/ProductController.java` | Added `PUT /api/products/{id}` endpoint |
