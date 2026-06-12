package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.GetProductUseCase;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductService implements GetProductUseCase {

    private final LoadProductPort loadProductPort;
    private final LoadCategoryPort loadCategoryPort;

    @Override
    public ProductView getById(UUID productId) {
        var product = loadProductPort.loadById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return toView(product);
    }

    @Override
    public List<ProductView> getByStoreId(UUID storeId) {
        return loadProductPort.loadByStoreId(storeId).stream()
                .map(this::toView)
                .toList();
    }

    private ProductView toView(Product product) {
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

        return new ProductView(
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
                new ProductView.StockInfo(stock.getTotalQuantity(), stock.getAvailableQuantity(), stock.getReservedQuantity()),
                product.getImages().stream()
                        .map(i -> new ProductView.ImageInfo(i.getId(), i.getUrl(), i.getPosition(), i.isPrimary()))
                        .toList(),
                optionInfos,
                variantViews,
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
