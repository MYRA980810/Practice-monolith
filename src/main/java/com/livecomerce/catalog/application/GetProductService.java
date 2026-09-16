package com.livecomerce.catalog.application;

import com.livecomerce.catalog.LoadProductRatingPort;
import com.livecomerce.catalog.LoadProductRatingPort.ProductRatingSummary;
import com.livecomerce.catalog.LoadProductSalesPort;
import com.livecomerce.catalog.application.port.in.GetProductUseCase;
import com.livecomerce.catalog.application.port.in.ProductFilter;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.query.ProductView;
import com.livecomerce.catalog.application.query.VariantView;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductOption;
import com.livecomerce.catalog.domain.ProductVariant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductService implements GetProductUseCase {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final LoadProductPort loadProductPort;
    private final LoadCategoryPort loadCategoryPort;
    private final LoadProductRatingPort loadProductRatingPort;
    private final LoadProductSalesPort loadProductSalesPort;

    @Override
    public ProductView getById(UUID productId) {
        var product = loadProductPort.loadById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        var ids = Set.of(productId);
        var ratings = loadRatingsSafely(ids);
        var sales = loadSalesSafely(ids);
        return toView(product, ratings, sales);
    }

    @Override
    public List<ProductView> getByStoreId(UUID storeId) {
        var products = loadProductPort.loadByStoreId(storeId);
        var ids = products.stream().map(Product::getId).collect(Collectors.toSet());
        var ratings = loadRatingsSafely(ids);
        var sales = loadSalesSafely(ids);
        return products.stream()
                .map(p -> toView(p, ratings, sales))
                .toList();
    }

    @Override
    public List<ProductView> listWithFilters(ProductFilter filter) {
        var products = loadProductPort.loadByFilter(filter);
        var ids = products.stream().map(Product::getId).collect(Collectors.toSet());
        var ratings = loadRatingsSafely(ids);
        var sales = loadSalesSafely(ids);
        return products.stream()
                .map(p -> toView(p, ratings, sales))
                .toList();
    }

    @Override
    public Page<ProductView> browse(ProductFilter filter, Pageable pageable) {
        var page = loadProductPort.browsePublic(filter, pageable);
        var ids = page.getContent().stream().map(Product::getId).collect(Collectors.toSet());
        var ratings = loadRatingsSafely(ids);
        var sales = loadSalesSafely(ids);
        return page.map(p -> toView(p, ratings, sales));
    }

    // Every read path here must degrade gracefully if the review module is
    // unavailable: a failure loading rating summaries should only blank out that
    // field, not fail the whole response (mirrors StoreController's
    // loadRatingsSafely/loadRanksSafely/loadFollowerCountsSafely per-loader
    // isolation pattern). Applied uniformly, not just on the public browse path —
    // a seller-facing getById/getByStoreId/listWithFilters call is no less exposed
    // to a review-module outage than the public endpoint is.
    private Map<UUID, ProductRatingSummary> loadRatingsSafely(Set<UUID> productIds) {
        try {
            return loadProductRatingPort.loadSummaries(productIds);
        } catch (Exception e) {
            log.error("Failed to load rating summaries for {} products; defaulting ratings for this response", productIds.size(), e);
            return Map.of();
        }
    }

    // Same rationale as loadRatingsSafely above, for the analytics cross-module read.
    private Map<UUID, Long> loadSalesSafely(Set<UUID> productIds) {
        try {
            return loadProductSalesPort.loadSoldCounts(productIds);
        } catch (Exception e) {
            log.error("Failed to load sold counts for {} products; defaulting soldCount to 0 for this response", productIds.size(), e);
            return Map.of();
        }
    }

    private ProductView toView(Product product, Map<UUID, ProductRatingSummary> ratings, Map<UUID, Long> sales) {
        var category = product.getCategoryId() != null
                ? loadCategoryPort.loadById(product.getCategoryId()).orElse(null)
                : null;

        var dv    = product.defaultVariant();
        var stock = dv.getStock();

        var optionInfos = product.getOptions().stream()
                .map(this::toOptionInfo)
                .toList();

        var variantViews = product.getVariants().stream()
                .map(v -> toVariantView(v, product.getBasePrice()))
                .toList();

        var rating = ratings.get(product.getId());
        var soldCount = sales.getOrDefault(product.getId(), 0L);

        return new ProductView(
                product.getId(),
                product.getStoreId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getCompareAtPrice(),
                discountLabel(product.getBasePrice(), product.getCompareAtPrice()),
                product.getCurrency(),
                dv.getSku(),
                product.isActive(),
                product.isPaused(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                new ProductView.StockInfo(stock.getTotalQuantity(), stock.getAvailableQuantity(), stock.getReservedQuantity()),
                stockLabel(stock.getAvailableQuantity()),
                soldCount,
                product.getImages().stream()
                        .map(i -> new ProductView.ImageInfo(i.getId(), i.getUrl(), i.getPosition(), i.isPrimary()))
                        .toList(),
                optionInfos,
                variantViews,
                rating != null ? rating.averageRating() : 0.0,
                rating != null ? rating.reviewCount() : 0L,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    /**
     * "-N%" when {@code compareAtPrice} is a genuine "was" price (strictly
     * greater than the current {@code basePrice}); {@code null} otherwise —
     * including when {@code compareAtPrice} is missing or is corrupt data
     * (equal to or lower than basePrice), which must never render as a
     * negative or zero discount.
     */
    static String discountLabel(BigDecimal basePrice, BigDecimal compareAtPrice) {
        if (compareAtPrice == null || compareAtPrice.compareTo(basePrice) <= 0) {
            return null;
        }
        var percentOff = compareAtPrice.subtract(basePrice)
                .divide(compareAtPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP);
        return "-" + percentOff + "%";
    }

    /** "Últimas unidades" (1-5), "Sin stock" (0), or null (>5). */
    static String stockLabel(int availableQuantity) {
        if (availableQuantity == 0) {
            return "Sin stock";
        }
        if (availableQuantity <= LOW_STOCK_THRESHOLD) {
            return "Últimas unidades";
        }
        return null;
    }

    private ProductView.OptionInfo toOptionInfo(ProductOption option) {
        var values = option.getValues().stream()
                .map(v -> new ProductView.OptionInfo.OptionValueInfo(v.getValue(), v.getSwatchHex()))
                .toList();
        return new ProductView.OptionInfo(option.getId(), option.getName(), values);
    }

    static VariantView toVariantView(ProductVariant v, BigDecimal basePrice) {
        var optionValues = v.getOptionValues().stream()
                .map(ov -> new VariantView.OptionValueInfo(ov.getOption().getName(), ov.getValue()))
                .toList();
        var s = v.getStock();
        return new VariantView(
                v.getId(),
                v.getProduct().getId(),
                v.getSku(),
                v.getPriceOverride(),
                v.effectivePrice(basePrice),
                v.isDefault(),
                v.getPosition(),
                optionValues,
                new VariantView.StockInfo(s.getTotalQuantity(), s.getAvailableQuantity(), s.getReservedQuantity())
        );
    }
}
