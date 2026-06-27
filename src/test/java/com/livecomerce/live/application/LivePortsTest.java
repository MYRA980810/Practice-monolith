package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.*;
import com.livecomerce.live.domain.LiveContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compilation test that verifies the shape of all command records.
 * These tests will fail at compile time if the port interface contracts are broken.
 */
class LivePortsTest {

    @Test
    void createLiveCommand_hasCorrectFields() {
        var sellerId = UUID.randomUUID();
        var storeId  = UUID.randomUUID();
        var cmd = new CreateLiveUseCase.CreateLiveCommand(
                sellerId, storeId, LiveContext.STORE, "Test Live", null, Instant.now(), 60);

        assertThat(cmd.sellerId()).isEqualTo(sellerId);
        assertThat(cmd.storeId()).isEqualTo(storeId);
        assertThat(cmd.context()).isEqualTo(LiveContext.STORE);
        assertThat(cmd.title()).isEqualTo("Test Live");
    }

    @Test
    void startLiveCommand_hasCorrectFields() {
        var liveId   = UUID.randomUUID();
        var sellerId = UUID.randomUUID();
        var cmd = new StartLiveUseCase.StartLiveCommand(liveId, sellerId, "uid-123");

        assertThat(cmd.liveId()).isEqualTo(liveId);
        assertThat(cmd.sellerId()).isEqualTo(sellerId);
        assertThat(cmd.rtcUid()).isEqualTo("uid-123");
    }

    @Test
    void endLiveCommand_hasCorrectFields() {
        var liveId   = UUID.randomUUID();
        var sellerId = UUID.randomUUID();
        var cmd = new EndLiveUseCase.EndLiveCommand(liveId, sellerId);

        assertThat(cmd.liveId()).isEqualTo(liveId);
        assertThat(cmd.sellerId()).isEqualTo(sellerId);
    }

    @Test
    void cancelLiveCommand_hasCorrectFields() {
        var liveId   = UUID.randomUUID();
        var sellerId = UUID.randomUUID();
        var cmd = new CancelLiveUseCase.CancelLiveCommand(liveId, sellerId);

        assertThat(cmd.liveId()).isEqualTo(liveId);
        assertThat(cmd.sellerId()).isEqualTo(sellerId);
    }

    @Test
    void addCatalogProductCommand_hasCorrectFields() {
        var liveId    = UUID.randomUUID();
        var sellerId  = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var variantId = UUID.randomUUID();
        var cmd = new AddCatalogProductUseCase.AddCatalogProductCommand(
                liveId, sellerId, productId, variantId, "T-Shirt",
                new BigDecimal("299.00"), "MXN", 100);

        assertThat(cmd.liveId()).isEqualTo(liveId);
        assertThat(cmd.nameSnapshot()).isEqualTo("T-Shirt");
        assertThat(cmd.stockAllocated()).isEqualTo(100);
    }

    @Test
    void addHotProductCommand_hasCorrectFields() {
        var liveId   = UUID.randomUUID();
        var sellerId = UUID.randomUUID();
        var cmd = new AddHotProductUseCase.AddHotProductCommand(
                liveId, sellerId, "Mystery Box", new BigDecimal("99.00"), "MXN", 50);

        assertThat(cmd.liveId()).isEqualTo(liveId);
        assertThat(cmd.name()).isEqualTo("Mystery Box");
        assertThat(cmd.stockAllocated()).isEqualTo(50);
    }

    @Test
    void pinProductCommand_hasCorrectFields() {
        var liveId        = UUID.randomUUID();
        var liveProductId = UUID.randomUUID();
        var sellerId      = UUID.randomUUID();
        var cmd = new PinProductUseCase.PinProductCommand(liveId, liveProductId, sellerId);

        assertThat(cmd.liveId()).isEqualTo(liveId);
        assertThat(cmd.liveProductId()).isEqualTo(liveProductId);
        assertThat(cmd.sellerId()).isEqualTo(sellerId);
    }

    @Test
    void reorderProductsCommand_hasCorrectFields() {
        var liveId   = UUID.randomUUID();
        var sellerId = UUID.randomUUID();
        var ids      = List.of(UUID.randomUUID(), UUID.randomUUID());
        var cmd = new ReorderProductsUseCase.ReorderProductsCommand(liveId, sellerId, ids);

        assertThat(cmd.orderedProductIds()).hasSize(2);
    }

    @Test
    void buyLiveProductCommand_hasCorrectFields() {
        var liveId        = UUID.randomUUID();
        var liveProductId = UUID.randomUUID();
        var buyerId       = UUID.randomUUID();
        var cmd = new BuyLiveProductUseCase.BuyLiveProductCommand(liveId, liveProductId, buyerId, 3);

        assertThat(cmd.quantity()).isEqualTo(3);
    }

    @Test
    void exitLiveCommand_hasCorrectFields() {
        var liveId  = UUID.randomUUID();
        var buyerId = UUID.randomUUID();
        var cmd = new ExitLiveUseCase.ExitLiveCommand(liveId, buyerId, "Av. Corrientes 1234", null);

        assertThat(cmd.shippingAddress()).isEqualTo("Av. Corrientes 1234");
        assertThat(cmd.stripePaymentMethodId()).isNull();
    }
}
