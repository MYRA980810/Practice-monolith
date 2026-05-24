package com.livecomerce.catalog.api;

import com.livecomerce.catalog.application.ProductNotFoundException;
import com.livecomerce.catalog.application.port.in.AddProductImageUseCase;
import com.livecomerce.catalog.application.port.in.AddStockUseCase;
import com.livecomerce.catalog.application.port.in.CreateProductUseCase;
import com.livecomerce.catalog.application.port.in.GetProductUseCase;
import com.livecomerce.catalog.domain.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("null")
@WebMvcTest(
        controllers = ProductController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class, OAuth2ClientAutoConfiguration.class, OAuth2ClientWebSecurityAutoConfiguration.class}
)
class ProductControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean CreateProductUseCase createProductUseCase;
    @MockitoBean GetProductUseCase getProductUseCase;
    @MockitoBean AddStockUseCase addStockUseCase;
    @MockitoBean AddProductImageUseCase addProductImageUseCase;

    private static final UUID STORE_ID   = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private static Product buildProduct() {
        return Product.create(STORE_ID, "Remera Básica", "Descripción", new BigDecimal("150.00"), "MXN", "SKU-001");
    }

    // --- POST /api/products ---

    @Test
    void create_withValidRequest_returns201WithProduct() throws Exception {
        when(createProductUseCase.create(any())).thenReturn(buildProduct());

        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "name": "Remera Básica",
                                  "description": "Descripción",
                                  "basePrice": 150.00,
                                  "currency": "MXN",
                                  "sku": "SKU-001"
                                }
                                """.formatted(STORE_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Remera Básica"))
                .andExpect(jsonPath("$.currency").value("MXN"))
                .andExpect(jsonPath("$.stock.totalQuantity").value(0))
                .andExpect(jsonPath("$.images").isArray());
    }

    @Test
    void create_withMissingName_returns400() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId": "%s", "basePrice": 100.00}
                                """.formatted(STORE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void create_withMissingStoreId_returns400() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Remera", "basePrice": 100.00}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withNegativePrice_returns400() throws Exception {
        mvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeId": "%s", "name": "Remera", "basePrice": -10.00}
                                """.formatted(STORE_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    // --- GET /api/products/{id} ---

    @Test
    void getById_whenProductExists_returns200() throws Exception {
        when(getProductUseCase.getById(any())).thenReturn(buildProduct());

        mvc.perform(get("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Remera Básica"))
                .andExpect(jsonPath("$.sku").value("SKU-001"));
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(getProductUseCase.getById(any())).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mvc.perform(get("/api/products/{id}", PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://livecomerce.com/errors/product-not-found"));
    }

    // --- GET /api/products?storeId= ---

    @Test
    void getByStore_returnsProductList() throws Exception {
        var p1 = buildProduct();
        var p2 = Product.create(STORE_ID, "Pantalón", null, new BigDecimal("200.00"), "MXN", null);
        when(getProductUseCase.getByStoreId(STORE_ID)).thenReturn(List.of(p1, p2));

        mvc.perform(get("/api/products").param("storeId", STORE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // --- POST /api/products/{id}/stock ---

    @Test
    void addStock_withValidQty_returns200() throws Exception {
        var product = buildProduct();
        product.addStock(20);
        when(addStockUseCase.addStock(any())).thenReturn(product);

        mvc.perform(post("/api/products/{id}/stock", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock.totalQuantity").value(20))
                .andExpect(jsonPath("$.stock.availableQuantity").value(20));
    }

    @Test
    void addStock_withZeroQty_returns400() throws Exception {
        mvc.perform(post("/api/products/{id}/stock", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addStock_whenProductNotFound_returns404() throws Exception {
        when(addStockUseCase.addStock(any())).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mvc.perform(post("/api/products/{id}/stock", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 10}
                                """))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/products/{id}/images ---

    @Test
    void addImage_withValidRequest_returns200() throws Exception {
        var product = buildProduct();
        when(addProductImageUseCase.addImage(any())).thenReturn(product);

        mvc.perform(post("/api/products/{id}/images", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "https://cdn.example.com/img.jpg", "position": 0, "primary": true}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void addImage_withBlankUrl_returns400() throws Exception {
        mvc.perform(post("/api/products/{id}/images", PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"url": "", "position": 0, "primary": false}
                                """))
                .andExpect(status().isBadRequest());
    }
}
