package com.livecomerce.catalog.api;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductImage;
import com.livecomerce.catalog.domain.ProductOptionValue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID storeId,
        String name,
        String description,
        BigDecimal basePrice,
        String currency,
        String sku,
        boolean active,
        UUID categoryId,
        String categoryName,
        StockInfo stock,
        List<ImageInfo> images,
        List<OptionInfo> options,
        List<VariantResponse> variants,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record StockInfo(int totalQuantity, int availableQuantity, int reservedQuantity) {}

    public record ImageInfo(UUID id, String url, int position, boolean primary) {
        static ImageInfo from(ProductImage image) {
            return new ImageInfo(image.getId(), image.getUrl(), image.getPosition(), image.isPrimary());
        }
    }

    public record OptionInfo(UUID id, String name, List<String> values) {}

    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    public static ProductResponse from(Product product, Category category) {
        var dv    = product.defaultVariant();
        var stock = dv.getStock();

        var optionInfos = product.getOptions().stream()
                .map(o -> new OptionInfo(
                        o.getId(),
                        o.getName(),
                        o.getValues().stream().map(ProductOptionValue::getValue).toList()))
                .toList();

        var variantResponses = product.getVariants().stream()
                .map(v -> VariantResponse.from(v, product.getBasePrice()))
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getStoreId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getCurrency(),
                dv.getSku(),
                product.isActive(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                new StockInfo(stock.getTotalQuantity(), stock.getAvailableQuantity(), stock.getReservedQuantity()),
                product.getImages().stream().map(ImageInfo::from).toList(),
                optionInfos,
                variantResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
