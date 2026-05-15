# Refactor: Product Listing — Public Endpoint vs Seller Endpoint

**Date:** 2026-05-14
**Module:** `catalog`, `auth`
**Author:** Christian Marquez
**Status:** Completed

---

## Context

The existing `GET /api/products?storeId=...` endpoint required authentication because the security config defaulted all unmatched routes to `authenticated()`. This conflicted with the product's intended access model:

- **Buyers and spectators** (unauthenticated users) need to browse a store's product catalog.
- **Sellers** need to view their own products without having to know or supply their store's UUID.

Both needs were being forced through the same endpoint, which served neither correctly. The endpoint required manual `storeId` input even for sellers — the same friction addressed in the previous refactor for product creation.

---

## Changes

### 1. `ProductController.java`

Added a new `GET /api/products/me` endpoint exclusively for sellers. The existing public endpoint was left intact.

```java
@GetMapping("/me")
@PreAuthorize("hasRole('SELLER')")
ResponseEntity<List<ProductResponse>> getMyProducts(@AuthenticationPrincipal UserPrincipal principal) {
    var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
    var products = getProductUseCase.getByStoreId(storeId).stream()
            .map(ProductResponse::from)
            .toList();
    return ResponseEntity.ok(products);
}
```

The `storeId` is resolved from the authenticated principal via `GetStoreUseCase`, following the same pattern established during the product creation refactor.

---

### 2. `SecurityConfig.java`

Registered `GET /api/products` and `GET /api/products/{id}` as public routes so unauthenticated users can browse without a token.

```java
.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/{id}").permitAll()
```

`HttpMethod.GET` is scoped explicitly so that `POST /api/products` and other mutating operations remain protected by the default `anyRequest().authenticated()` rule.

---

## Endpoint Summary

| Endpoint | Auth | Role | Use case |
|---|---|---|---|
| `GET /api/products?storeId=...` | Not required | — | Public storefront browsing |
| `GET /api/products/{id}` | Not required | — | Public single product view |
| `GET /api/products/me` | Required | `SELLER` | Seller views their own catalog |
| `POST /api/products` | Required | `SELLER` | Create product |
| `POST /api/products/{id}/stock` | Required | `SELLER` | Add stock |
| `POST /api/products/{id}/images` | Required | `SELLER` | Add image |

---

## Problems Encountered

### Problem 1 — `GET /api/products/me` matches the `{id}` path pattern

**Risk**

Spring Security processes `requestMatchers` rules top-down. The rule:

```java
.requestMatchers(HttpMethod.GET, "/api/products/{id}").permitAll()
```

matches any single path segment after `/api/products`, including the literal string `me`. This means the filter chain would allow unauthenticated requests through to `GET /api/products/me`.

**Resolution**

`/api/products/me` was intentionally excluded from the `permitAll` declaration. The security filter lets the request pass through, but `@PreAuthorize("hasRole('SELLER')")` at the method level intercepts it. An unauthenticated request hitting `/api/products/me` receives `401 Unauthorized`. This is the correct behavior and leverages the two-layer security model Spring Security provides: filter chain + method security.

**Key distinction:** `permitAll` at the filter level means "do not block at the filter" — it does not mean "bypass method-level security." Both layers are independent.

---

## Architecture Notes

### Why `@PreAuthorize` and not a second `requestMatchers` rule

Spring Security evaluates `requestMatchers` rules in declaration order. Adding a specific rule for `GET /api/products/me` before the `{id}` wildcard rule would work at the filter level, but the method-level `@PreAuthorize` already provides a cleaner, more explicit contract. The authorization intent is visible directly at the handler method, which is easier to audit and maintain.

### Why `HttpMethod.GET` is scoped explicitly

Without `HttpMethod` scoping, a `permitAll` on `/api/products` would also permit `POST /api/products`, stripping the authentication requirement from product creation. Scoping to `GET` is mandatory here.

### Pattern established

Both `POST /api/products` (create) and `GET /api/products/me` (list own) resolve `storeId` from the authenticated principal via `GetStoreUseCase.getStoreIdByUserId()`. This is now the consistent pattern for all seller operations in the `catalog` module.

---

## Files Changed

| File | Change |
|---|---|
| `catalog/api/ProductController.java` | Added `GET /api/products/me` endpoint |
| `auth/infrastructure/security/SecurityConfig.java` | Added `permitAll` for public product GET routes |
