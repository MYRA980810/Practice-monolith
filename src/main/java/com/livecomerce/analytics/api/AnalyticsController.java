package com.livecomerce.analytics.api;

import com.livecomerce.analytics.application.port.in.GetChannelMetricsUseCase;
import com.livecomerce.analytics.application.port.in.GetDeadStockUseCase;
import com.livecomerce.analytics.application.port.in.GetProductRotationUseCase;
import com.livecomerce.analytics.application.port.in.GetSalesMetricsUseCase;
import com.livecomerce.analytics.domain.ChannelComparison;
import com.livecomerce.analytics.domain.DeadStockItem;
import com.livecomerce.analytics.domain.LivePerformance;
import com.livecomerce.analytics.domain.PaymentMethodBreakdown;
import com.livecomerce.analytics.domain.RevenueByPeriod;
import com.livecomerce.analytics.domain.SalesSummary;
import com.livecomerce.analytics.domain.StockAlert;
import com.livecomerce.analytics.domain.TopProduct;
import com.livecomerce.analytics.domain.Period;
import com.livecomerce.shared.UserPrincipal;
import com.livecomerce.store.application.port.in.GetStoreUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
class AnalyticsController {

    private final GetStoreUseCase getStoreUseCase;
    private final GetSalesMetricsUseCase getSalesMetricsUseCase;
    private final GetChannelMetricsUseCase getChannelMetricsUseCase;
    private final GetProductRotationUseCase getProductRotationUseCase;
    private final GetDeadStockUseCase getDeadStockUseCase;

    // === Sales ===

    @GetMapping("/sales/summary")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<SalesSummary> getSalesSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var summary = getSalesMetricsUseCase.getSummary(
                new GetSalesMetricsUseCase.SalesQuery(storeId, from, to));
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/sales/revenue")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<RevenueByPeriod>> getRevenueByPeriod(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Period period,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var revenue = getSalesMetricsUseCase.getRevenueByPeriod(
                new GetSalesMetricsUseCase.RevenueQuery(storeId, period, from, to));
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/sales/payment-methods")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<PaymentMethodBreakdown>> getPaymentMethodBreakdown(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var breakdown = getSalesMetricsUseCase.getPaymentMethodBreakdown(
                new GetSalesMetricsUseCase.SalesQuery(storeId, from, to));
        return ResponseEntity.ok(breakdown);
    }

    @GetMapping("/sales/commission")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<BigDecimal> getCommission(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var commission = getSalesMetricsUseCase.getCommission(
                new GetSalesMetricsUseCase.SalesQuery(storeId, from, to));
        return ResponseEntity.ok(commission);
    }

    // === Channels ===

    @GetMapping("/channels/comparison")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<ChannelComparison> getChannelComparison(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var comparison = getChannelMetricsUseCase.getChannelComparison(storeId, from, to);
        return ResponseEntity.ok(comparison);
    }

    @GetMapping("/channels/lives")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<LivePerformance>> getLivePerformances(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var performances = getChannelMetricsUseCase.getLivePerformances(storeId, from, to);
        return ResponseEntity.ok(performances);
    }

    // === Products ===

    @GetMapping("/products/top")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<TopProduct>> getTopProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "REVENUE") String metric,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var topProducts = getProductRotationUseCase.getTopProducts(
                new GetProductRotationUseCase.TopProductsQuery(
                        storeId, metric, categoryId, limit, from, to));
        return ResponseEntity.ok(topProducts);
    }

    // === Stock ===

    @GetMapping("/stock/dead")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<DeadStockItem>> getDeadStock(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "90") int notSoldInDays) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var deadStock = getDeadStockUseCase.getDeadStock(storeId, notSoldInDays);
        return ResponseEntity.ok(deadStock);
    }

    @GetMapping("/stock/alerts")
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<List<StockAlert>> getStockAlerts(
            @AuthenticationPrincipal UserPrincipal principal) {

        var storeId = getStoreUseCase.getStoreIdByUserId(principal.getUserId());
        var alerts = getDeadStockUseCase.getStockAlerts(storeId);
        return ResponseEntity.ok(alerts);
    }
}
