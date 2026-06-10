package com.livecomerce.catalog.api;

import com.livecomerce.catalog.domain.Category;
import com.livecomerce.catalog.domain.Product;
import com.livecomerce.catalog.domain.ProductImage;
import com.livecomerce.catalog.domain.Stock;

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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record StockInfo(int totalQuantity, int availableQuantity, int reservedQuantity) {
        static StockInfo from(Stock stock) {
            return new StockInfo(
                    stock.getTotalQuantity(),
                    stock.getAvailableQuantity(),
                    stock.getReservedQuantity()
            );
        }
    }

    public record ImageInfo(UUID id, String url, int position, boolean primary) {
        static ImageInfo from(ProductImage image) {
            return new ImageInfo(image.getId(), image.getUrl(), image.getPosition(), image.isPrimary());
        }
    }

    public static ProductResponse from(Product product) {
        return from(product, null);
    }

    public static ProductResponse from(Product product, Category category) {
        return new ProductResponse(
                product.getId(),
                product.getStoreId(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getCurrency(),
                product.getSku(),
                product.isActive(),
                product.getCategoryId(),
                category != null ? category.getName() : null,
                product.getStock() != null ? StockInfo.from(product.getStock()) : null,
                product.getImages().stream().map(ImageInfo::from).toList(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
