# Refactor: Product Creation — Store Resolution & Currency Default

**Date:** 2026-05-14
**Module:** `catalog`
**Author:** Christian Marquez
**Status:** Completed

---

## Context

The `CreateProductRequest` DTO required the client to supply two fields that the backend can resolve internally:

- `storeId` — the UUID of the store where the product would be created
- `currency` — the ISO 4217 currency code for the product's base price

Requiring `storeId` in the request introduces two concrete risks:

1. **Authorization gap.** A seller could supply the ID of a store they don't own, potentially creating products in a foreign store. The authenticated principal already carries the information needed to resolve the correct store.
2. **Unnecessary friction.** Clients are forced to cache and transmit an ID that the backend already knows.

Requiring `currency` unconditionally is also poor UX for a product initially distributed in Mexico, where all stores operate in Mexican Peso (MXN).

---

## Changes

### 1. `CreateProductRequest.java`

Removed the `storeId` field entirely. The `currency` field was kept but left optional (no `@NotNull`), allowing the backend to apply a regional default.

**Before**
```java
record CreateProductRequest(
    @NotNull UUID storeId,
    @NotBlank @Size(max = 255) String name,
    String description,
    @NotNull @DecimalMin("0.01") BigDecimal basePrice,
    @Size(max = 3) String currency,
    @Size(max = 100) String sku
) {}
```

**After**
```java
record CreateProductRequest(
    @NotBlank @Size(max = 255) String name,
    String description,
    @NotNull @DecimalMin("0.01") BigDecimal basePrice,
    @Size(max = 3) String currency,
    @Size(max = 100) String sku
) {}
```

---

### 2. `ProductController.java`

- Injected `GetStoreUseCase` to resolve the store from the authenticated user.
- Added `@AuthenticationPrincipal UserPrincipal principal` to the `create` endpoint, following the established pattern in `StoreController`.
- Applied `"MXN"` as the default currency when the client omits the field.

```java
private static final String DEFAULT_CURRENCY = "MXN";

ResponseEntity<ProductResponse> create(
        @Valid @RequestBody CreateProductRequest request,
        @AuthenticationPrincipal UserPrincipal principal) {

    var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
    var product = createProductUseCase.create(new CreateProductUseCase.CreateProductCommand(
            storeId,
            request.name(),
            request.description(),
            request.basePrice(),
            Objects.requireNonNullElse(request.currency(), DEFAULT_CURRENCY),
            request.sku()
    ));
    ...
}
```

---

### 3. `GetStoreUseCase.java` + `GetStoreService.java`

Added a new query method that returns only the store's `UUID`, keeping the `Store` domain entity contained within the `store` module.

```java
// GetStoreUseCase
UUID getStoreIdByUserId(UUID userId);

// GetStoreService
@Override
public UUID getStoreIdByUserId(UUID userId) {
    return getByUserId(userId).getId();
}
```

---

### 4. Spring Modulith boundary declarations

**`store/application/port/in/package-info.java`** — created:
```java
@org.springframework.modulith.NamedInterface("in")
package com.livecomerce.store.application.port.in;
```

**`catalog/package-info.java`** — updated:
```java
@org.springframework.modulith.ApplicationModule(
    displayName = "Catalog",
    allowedDependencies = "store::in"
)
package com.livecomerce.catalog;
```

---

## Problems Encountered

### Problem 1 — Spring Modulith module boundary violation on `GetStoreUseCase`

**Error**
```
Invalid reference to non-exposed type of module 'store'!
MODULITH_TYPE_REF_VIOLATION
```

**Cause**

Spring Modulith enforces that only types in a module's root package (`com.livecomerce.store`) are accessible to other modules by default. `GetStoreUseCase` lives in `com.livecomerce.store.application.port.in`, a nested subpackage, which is treated as module-internal.

**Resolution**

A `@NamedInterface("in")` annotation was added to the subpackage via `package-info.java`. This explicitly designates the package as a public contract of the `store` module. The `catalog` module then declared `allowedDependencies = "store::in"` in its own `package-info.java`, making the dependency explicit and verifiable by Modulith's architectural tests.

---

### Problem 2 — `Store` domain entity leaking across module boundary

**Error**
```
Invalid reference to non-exposed type of module 'store'!  (line 40)
```

**Cause**

Even after exposing `GetStoreUseCase`, calling `getByUserId()` returns a `Store` object from `com.livecomerce.store.domain` — another non-exposed subpackage. Java's `var` keyword does not prevent Modulith's static analysis from detecting the indirect type reference.

**Resolution**

A new method `getStoreIdByUserId(UUID userId)` was added to `GetStoreUseCase`. This method returns a plain `java.util.UUID`, a JDK type that carries no module coupling. The `Store` entity stays entirely within the `store` module; `catalog` never references it.

This is the correct approach: **cross-module queries should return primitive or shared types, not domain entities from the called module.**

---

### Problem 3 — Unused import after `storeId` removal

**Warning**
```
The import java.util.UUID is never used
```

**Cause**

Removing `storeId` from `CreateProductRequest` left the `UUID` import orphaned.

**Resolution**

Import removed.

---

## Architecture Notes

### Why the controller — not the use case — resolves the store

The `CreateProductCommand` intentionally continues to accept `storeId` as a `UUID`. This keeps the application layer decoupled from the authentication mechanism: the use case does not need to know about `SecurityContext`, `UserPrincipal`, or HTTP concerns. Resolution of identity-to-resource mapping is a responsibility of the API adapter layer (the controller), which is consistent with hexagonal architecture.

### Why `currency` was not moved to the `Store` entity

Moving `currency` to `Store` would be the correct long-term model if all products in a store share a single currency. That decision was deferred pending a business decision on whether multi-currency stores are in scope. For now, the field remains per-product with a regional default.

### SKU — no change

`sku` (Stock Keeping Unit) is a seller-defined identifier used for inventory management. It is intentionally optional and not system-generated, as sellers may or may not operate with SKU-based catalogs.

---

## Files Changed

| File | Change |
|---|---|
| `catalog/api/CreateProductRequest.java` | Removed `storeId` field and orphaned `UUID` import |
| `catalog/api/ProductController.java` | Added `GetStoreUseCase`, `UserPrincipal`, and default currency logic |
| `catalog/package-info.java` | Added `allowedDependencies = "store::in"` |
| `store/application/port/in/GetStoreUseCase.java` | Added `getStoreIdByUserId(UUID)` method |
| `store/application/GetStoreService.java` | Implemented `getStoreIdByUserId(UUID)` |
| `store/application/port/in/package-info.java` | Created — exposes package as `@NamedInterface("in")` |
