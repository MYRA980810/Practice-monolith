package com.livecomerce.live.application;

import com.livecomerce.live.LoadCatalogProductPort;
import com.livecomerce.live.LoadCatalogProductPort.CatalogProductLookup;
import com.livecomerce.live.LoadCatalogProductPort.CatalogProductSnapshot;
import com.livecomerce.live.LoadSellerStoreIdPort;
import com.livecomerce.live.application.port.in.AddCatalogProductUseCase.AddCatalogProductCommand;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class AddCatalogProductToLiveServiceTest {

    @Mock LoadLivePort           loadLivePort;
    @Mock SaveLiveProductPort    saveLiveProductPort;
    @Mock LoadSellerStoreIdPort  loadSellerStoreIdPort;
    @Mock LoadCatalogProductPort loadCatalogProductPort;
    @InjectMocks AddCatalogProductToLiveService sut;

    private static final UUID SELLER_ID  = UUID.randomUUID();
    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    private static CatalogProductSnapshot snapshot(UUID storeId, BigDecimal price, boolean active, boolean paused) {
        return new CatalogProductSnapshot(PRODUCT_ID, VARIANT_ID, storeId, "T-Shirt", price, "MXN",
                "https://cdn.test/catalog.jpg", active, paused);
    }

    private static CatalogProductSnapshot sellableSnapshot() {
        return snapshot(STORE_ID, new BigDecimal("199.00"), true, false);
    }

    private static AddCatalogProductCommand command(UUID liveId, UUID sellerId, String imageUrl) {
        return new AddCatalogProductCommand(liveId, sellerId, PRODUCT_ID, VARIANT_ID, 50, imageUrl);
    }

    private Live scheduledLive() {
        var live = Live.create(SELLER_ID, "Live", null, null, 60);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        return live;
    }

    private void sellerHasOpenStore() {
        when(loadSellerStoreIdPort.findOpenStoreIdBySellerId(SELLER_ID)).thenReturn(Optional.of(STORE_ID));
    }

    private void catalogReturns(CatalogProductSnapshot snapshot) {
        when(loadCatalogProductPort.loadForLive(PRODUCT_ID, VARIANT_ID)).thenReturn(CatalogProductLookup.found(snapshot));
    }

    // --- happy paths: snapshot comes from the catalog ---

    @Test
    void addCatalogProduct_ownProductInOpenStore_snapshotsCatalogData() {
        var live = scheduledLive();
        sellerHasOpenStore();
        catalogReturns(sellableSnapshot());
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.addCatalogProduct(command(live.getId(), SELLER_ID, "https://cdn.test/shirt.jpg"));

        assertThat(result.isHot()).isFalse();
        assertThat(result.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(result.getVariantId()).isEqualTo(VARIANT_ID);
        assertThat(result.getProductNameSnapshot()).isEqualTo("T-Shirt");
        assertThat(result.getPriceSnapshot()).isEqualByComparingTo("199.00");
        assertThat(result.getCurrencySnapshot()).isEqualTo("MXN");
        assertThat(result.getStockAllocated()).isEqualTo(50);
        assertThat(result.getImageUrl()).isEqualTo("https://cdn.test/shirt.jpg");
    }

    @Test
    void addCatalogProduct_variantPrice_isTakenFromCatalogSnapshot() {
        var live = scheduledLive();
        sellerHasOpenStore();
        catalogReturns(snapshot(STORE_ID, new BigDecimal("249.00"), true, false));
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.addCatalogProduct(command(live.getId(), SELLER_ID, null));

        assertThat(result.getPriceSnapshot()).isEqualByComparingTo("249.00");
    }

    @Test
    void addCatalogProduct_withoutImage_fallsBackToCatalogImage() {
        var live = scheduledLive();
        sellerHasOpenStore();
        catalogReturns(sellableSnapshot());
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.addCatalogProduct(command(live.getId(), SELLER_ID, null));

        assertThat(result.getImageUrl()).isEqualTo("https://cdn.test/catalog.jpg");
    }

    @Test
    void addCatalogProduct_emptyImage_fallsBackToCatalogImage() {
        var live = scheduledLive();
        sellerHasOpenStore();
        catalogReturns(sellableSnapshot());
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.addCatalogProduct(command(live.getId(), SELLER_ID, ""));

        assertThat(result.getImageUrl()).isEqualTo("https://cdn.test/catalog.jpg");
    }

    @Test
    void addCatalogProduct_whitespaceImage_fallsBackToCatalogImage() {
        var live = scheduledLive();
        sellerHasOpenStore();
        catalogReturns(sellableSnapshot());
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.addCatalogProduct(command(live.getId(), SELLER_ID, "   "));

        assertThat(result.getImageUrl()).isEqualTo("https://cdn.test/catalog.jpg");
    }

    @Test
    void addCatalogProduct_toLiveLive_succeeds() {
        var live = scheduledLive();
        live.start();
        sellerHasOpenStore();
        catalogReturns(sellableSnapshot());
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.addCatalogProduct(command(live.getId(), SELLER_ID, null));

        assertThat(result.getProductNameSnapshot()).isEqualTo("T-Shirt");
    }

    @Test
    void addCatalogProduct_toReconnectingLive_succeeds() {
        var live = scheduledLive();
        live.start();
        live.beginReconnecting();
        sellerHasOpenStore();
        catalogReturns(sellableSnapshot());
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = sut.addCatalogProduct(command(live.getId(), SELLER_ID, null));

        assertThat(result.getProductNameSnapshot()).isEqualTo("T-Shirt");
    }

    // --- live ownership / state (unchanged) ---

    @Test
    void addCatalogProduct_wrongSeller_throwsLiveNotOwned() {
        var live = scheduledLive();

        assertThatThrownBy(() -> sut.addCatalogProduct(command(live.getId(), UUID.randomUUID(), null)))
                .isInstanceOf(LiveNotOwnedBySellerException.class);
        verifyNoInteractions(loadSellerStoreIdPort, loadCatalogProductPort, saveLiveProductPort);
    }

    @Test
    void addCatalogProduct_toEndedLive_throwsIllegalState() {
        var live = scheduledLive();
        live.start();
        live.end();

        assertThatThrownBy(() -> sut.addCatalogProduct(command(live.getId(), SELLER_ID, null)))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(saveLiveProductPort);
    }

    // --- store ownership / store state: same HTTP answer, distinct internal log reason ---

    private static void assertLoggedRejection(CapturedOutput output, UUID liveId, String reason) {
        assertThat(output.getOut())
                .contains("reason=" + reason)
                .contains("liveId=" + liveId)
                .contains("sellerId=" + SELLER_ID)
                .contains("productId=" + PRODUCT_ID)
                .contains("variantId=" + VARIANT_ID);
    }

    @Test
    void addCatalogProduct_sellerWithoutOpenStore_throwsSellerStoreNotOpen(CapturedOutput output) {
        var live = scheduledLive();
        when(loadSellerStoreIdPort.findOpenStoreIdBySellerId(SELLER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.addCatalogProduct(command(live.getId(), SELLER_ID, null)))
                .isInstanceOfSatisfying(SellerStoreNotOpenException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(loadCatalogProductPort, saveLiveProductPort);
        assertLoggedRejection(output, live.getId(), "NO_OPEN_STORE");
    }

    @Test
    void addCatalogProduct_productOfAnotherSellersStore_throwsCatalogProductNotAvailable(CapturedOutput output) {
        var live = scheduledLive();
        sellerHasOpenStore();
        catalogReturns(snapshot(UUID.randomUUID(), new BigDecimal("199.00"), true, false));

        assertThatThrownBy(() -> sut.addCatalogProduct(command(live.getId(), SELLER_ID, null)))
                .isInstanceOfSatisfying(CatalogProductNotAvailableException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verifyNoInteractions(saveLiveProductPort);
        assertLoggedRejection(output, live.getId(), "PRODUCT_IN_ANOTHER_STORE");
    }

    @Test
    void addCatalogProduct_unknownProduct_throwsCatalogProductNotAvailable(CapturedOutput output) {
        var live = scheduledLive();
        sellerHasOpenStore();
        when(loadCatalogProductPort.loadForLive(PRODUCT_ID, VARIANT_ID))
                .thenReturn(CatalogProductLookup.productNotFound());

        assertThatThrownBy(() -> sut.addCatalogProduct(command(live.getId(), SELLER_ID, null)))
                .isInstanceOf(CatalogProductNotAvailableException.class);
        verifyNoInteractions(saveLiveProductPort);
        assertLoggedRejection(output, live.getId(), "PRODUCT_NOT_FOUND");
    }

    @Test
    void addCatalogProduct_variantNotInProduct_throwsCatalogProductNotAvailable(CapturedOutput output) {
        var live = scheduledLive();
        sellerHasOpenStore();
        when(loadCatalogProductPort.loadForLive(PRODUCT_ID, VARIANT_ID))
                .thenReturn(CatalogProductLookup.variantNotInProduct());

        assertThatThrownBy(() -> sut.addCatalogProduct(command(live.getId(), SELLER_ID, null)))
                .isInstanceOf(CatalogProductNotAvailableException.class);
        verifyNoInteractions(saveLiveProductPort);
        assertLoggedRejection(output, live.getId(), "VARIANT_NOT_IN_PRODUCT");
    }

    @Test
    void addCatalogProduct_missingAndForeignProduct_produceIdenticalClientError() {
        var live = scheduledLive();
        sellerHasOpenStore();
        when(loadCatalogProductPort.loadForLive(PRODUCT_ID, VARIANT_ID))
                .thenReturn(CatalogProductLookup.productNotFound())
                .thenReturn(CatalogProductLookup.found(
                        snapshot(UUID.randomUUID(), new BigDecimal("1.00"), true, false)));

        var missing = catchThrowableOfType(CatalogProductNotAvailableException.class,
                () -> sut.addCatalogProduct(command(live.getId(), SELLER_ID, null)));
        var foreign = catchThrowableOfType(CatalogProductNotAvailableException.class,
                () -> sut.addCatalogProduct(command(live.getId(), SELLER_ID, null)));

        assertThat(foreign.getMessage()).isEqualTo(missing.getMessage());
        assertThat(foreign.getStatus()).isEqualTo(missing.getStatus());
        assertThat(foreign.getType()).isEqualTo(missing.getType());
    }

    @Test
    void addCatalogProduct_pausedProduct_throwsCatalogProductNotAvailable(CapturedOutput output) {
        var live = scheduledLive();
        sellerHasOpenStore();
        catalogReturns(snapshot(STORE_ID, new BigDecimal("199.00"), true, true));

        assertThatThrownBy(() -> sut.addCatalogProduct(command(live.getId(), SELLER_ID, null)))
                .isInstanceOf(CatalogProductNotAvailableException.class)
                .hasMessageContaining("not available for sale");
        verifyNoInteractions(saveLiveProductPort);
        assertLoggedRejection(output, live.getId(), "PRODUCT_PAUSED");
    }

    @Test
    void addCatalogProduct_deactivatedProduct_throwsCatalogProductNotAvailable(CapturedOutput output) {
        var live = scheduledLive();
        sellerHasOpenStore();
        catalogReturns(snapshot(STORE_ID, new BigDecimal("199.00"), false, false));

        assertThatThrownBy(() -> sut.addCatalogProduct(command(live.getId(), SELLER_ID, null)))
                .isInstanceOf(CatalogProductNotAvailableException.class);
        verifyNoInteractions(saveLiveProductPort);
        assertLoggedRejection(output, live.getId(), "PRODUCT_INACTIVE");
    }
}
