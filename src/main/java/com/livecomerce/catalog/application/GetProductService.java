package com.livecomerce.catalog.application;

import com.livecomerce.catalog.LoadProductRatingPort;
import com.livecomerce.catalog.LoadProductRatingPort.ProductRatingSummary;
import com.livecomerce.catalog.application.port.in.GetProductUseCase;
import com.livecomerce.catalog.application.port.in.ProductFilter;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.LoadProductPort;
import com.livecomerce.catalog.application.query.ProductView;
import com.livecomerce.catalog.application.query.VariantView;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductOption;
import com.livecomerce.catalog.domain.ProductOptionValue;
import com.livecomerce.catalog.domain.ProductVariant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductService implements GetProductUseCase {

    private final LoadProductPort loadProductPort;
    private final LoadCategoryPort loadCategoryPort;
    private final LoadProductRatingPort loadProductRatingPort;

    @Override
    public ProductView getById(UUID productId) {
        var product = loadProductPort.loadById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        var ratings = loadProductRatingPort.loadSummaries(Set.of(productId));
        return toView(product, ratings);
    }

    @Override
    public List<ProductView> getByStoreId(UUID storeId) {
        var products = loadProductPort.loadByStoreId(storeId);
        var ratings = loadProductRatingPort.loadSummaries(
                products.stream().map(Product::getId).collect(Collectors.toSet()));
        return products.stream()
                .map(p -> toView(p, ratings))
                .toList();
    }

    @Override
    public List<ProductView> listWithFilters(ProductFilter filter) {
        var products = loadProductPort.loadByFilter(filter);
        var ratings = loadProductRatingPort.loadSummaries(
                products.stream().map(Product::getId).collect(Collectors.toSet()));
        return products.stream()
                .map(p -> toView(p, ratings))
                .toList();
    }

    private ProductView toView(Product product, Map<UUID, ProductRatingSummary> ratings) {
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

        return new ProductView(
                product.getId(),
                product.getStoreId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getCurrency(),
                dv.getSku(),
                product.isActive(),
                product.isPaused(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                new ProductView.StockInfo(stock.getTotalQuantity(), stock.getAvailableQuantity(), stock.getReservedQuantity()),
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

    private ProductView.OptionInfo toOptionInfo(ProductOption option) {
        var values = option.getValues().stream()
                .map(ProductOptionValue::getValue)
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
