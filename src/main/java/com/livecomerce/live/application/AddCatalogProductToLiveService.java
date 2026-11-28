package com.livecomerce.live.application;

import com.livecomerce.live.LoadCatalogProductPort;
import com.livecomerce.live.LoadCatalogProductPort.LookupStatus;
import com.livecomerce.live.LoadSellerStoreIdPort;
import com.livecomerce.live.application.port.in.AddCatalogProductUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Adds a product of the seller's own store catalog to their live. The seller must have a
 * store open for sales and the product must belong to it and be buyer-visible (active and
 * not paused). Name, price and currency are snapshotted from the catalog, never the client.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AddCatalogProductToLiveService implements AddCatalogProductUseCase {

    private static final Logger log = LoggerFactory.getLogger(AddCatalogProductToLiveService.class);

    private final LoadLivePort           loadLivePort;
    private final SaveLiveProductPort    saveLiveProductPort;
    private final LoadSellerStoreIdPort  loadSellerStoreIdPort;
    private final LoadCatalogProductPort loadCatalogProductPort;

    @Override
    public LiveProduct addCatalogProduct(AddCatalogProductCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());
        verifyLiveActiveForProducts(live);

        var storeId = loadSellerStoreIdPort.findOpenStoreIdBySellerId(live.getSellerId()).orElse(null);
        if (storeId == null) {
            logRejection(command, "NO_OPEN_STORE");
            throw new SellerStoreNotOpenException(live.getSellerId());
        }

        // Missing and foreign products share one client-facing error (anti-probing);
        // only the server log tells them apart.
        var lookup = loadCatalogProductPort.loadForLive(command.productId(), command.variantId());
        if (lookup.status() != LookupStatus.FOUND) {
            logRejection(command, lookup.status().name());
            throw CatalogProductNotAvailableException.notInSellerStore(command.productId(), command.variantId());
        }
        var product = lookup.snapshot();
        if (!product.storeId().equals(storeId)) {
            logRejection(command, "PRODUCT_IN_ANOTHER_STORE");
            throw CatalogProductNotAvailableException.notInSellerStore(command.productId(), command.variantId());
        }
        if (!product.sellable()) {
            logRejection(command, product.active() ? "PRODUCT_PAUSED" : "PRODUCT_INACTIVE");
            throw CatalogProductNotAvailableException.notSellable(product.productId());
        }

        var imageUrl = command.imageUrl() != null && !command.imageUrl().isBlank()
                ? command.imageUrl()
                : product.imageUrl();

        var lp = LiveProduct.forCatalogProduct(
                live, product.productId(), product.variantId(),
                product.name(), product.price(), product.currency(),
                command.stockAllocated(), imageUrl);

        return saveLiveProductPort.save(lp);
    }

    private static void logRejection(AddCatalogProductCommand command, String reason) {
        log.warn("Catalog product rejected for live: reason={} liveId={} sellerId={} productId={} variantId={}",
                reason, command.liveId(), command.sellerId(), command.productId(), command.variantId());
    }

    private void verifySeller(Live live, UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }

    private void verifyLiveActiveForProducts(Live live) {
        if (live.getStatus() != LiveStatus.SCHEDULED && live.getStatus() != LiveStatus.LIVE
                && live.getStatus() != LiveStatus.RECONNECTING) {
            throw new IllegalStateException(
                    "Cannot add products to live in status: " + live.getStatus());
        }
    }
}
