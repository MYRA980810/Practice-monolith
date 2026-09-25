package com.livecomerce.live;

import java.util.UUID;

/**
 * Consumer-owned port: {@code live} declares what it needs to know about catalog
 * categories and {@code catalog} implements it with a package-private adapter.
 * Living in live's root package keeps the dependency pointing catalog → live,
 * so live never imports catalog and no Modulith cycle is created
 * (same pattern as {@link LoadSellerNamesPort}).
 */
public interface CategoryLookupPort {

    /** {@code true} only if the category exists and is ACTIVE; missing or pending categories are not. */
    boolean isActive(UUID categoryId);
}
