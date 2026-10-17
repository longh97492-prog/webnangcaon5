package com.example.supplychain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.example.supplychain.entity.Product;
import com.example.supplychain.entity.Warehouse;
import com.example.supplychain.repository.InventoryRepository;
import com.example.supplychain.repository.ProductRepository;
import com.example.supplychain.repository.WarehouseRepository;

@SpringBootTest(properties = {
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:stock_api_test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "WAREHOUSE_STAFF")
class StockApiValidationTest {

    private static final long MISSING_ID = 999_999L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Long productId;
    private Long warehouseId;

    @BeforeEach
    void setUp() {
        Product product = new Product();
        product.setSku("SKU-TEST");
        product.setName("Test product");
        product.setUnit("pcs");
        product.setPrice(BigDecimal.TEN);
        productId = productRepository.save(product).getId();

        Warehouse warehouse = new Warehouse();
        warehouse.setCode("WH-TEST");
        warehouse.setName("Test warehouse");
        warehouse.setAddress("Ha Noi");
        warehouseId = warehouseRepository.save(warehouse).getId();
    }

    private ResultActions call(
            String url,
            Object product,
            Object warehouse,
            Object quantity) throws Exception {

        String json = """
                {
                    "receiptCode": "TC-001",
                    "productId": %s,
                    "warehouseId": %s,
                    "quantity": %s
                }
                """.formatted(product, warehouse, quantity);

        return mvc.perform(post(url)
                .with(csrf().asHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private int stock() {
        return inventoryRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .map(inventory -> inventory.getQuantity())
                .orElse(0);
    }

    @Test
    void tc01_importValid() throws Exception {
        call("/api/imports", productId, warehouseId, 10)
                .andExpect(status().isOk());

        assertEquals(10, stock());
    }

    @Test
    void tc02_exportValid() throws Exception {
        call("/api/imports", productId, warehouseId, 10)
                .andExpect(status().isOk());

        call("/api/exports", productId, warehouseId, 5)
                .andExpect(status().isOk());

        assertEquals(5, stock());
    }

    @Test
    void tc03_productNotFound() throws Exception {
        call("/api/imports", MISSING_ID, warehouseId, 5)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void tc04_warehouseNotFound() throws Exception {
        call("/api/imports", productId, MISSING_ID, 5)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void tc05_quantityZero() throws Exception {
        call("/api/imports", productId, warehouseId, 0)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void tc06_quantityNegative() throws Exception {
        call("/api/imports", productId, warehouseId, -5)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void tc07_exportExceedsStock() throws Exception {
        call("/api/imports", productId, warehouseId, 10)
                .andExpect(status().isOk());

        call("/api/exports", productId, warehouseId, 1_000_000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUSINESS_CONFLICT"));

        assertEquals(10, stock());
    }

    @Test
    @WithAnonymousUser
    void tc08_unauthenticated() throws Exception {
        call("/api/imports", productId, warehouseId, 5)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc09_productIdMissing() throws Exception {
        call("/api/exports", null, warehouseId, 5)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void tc10_quantityWrongType() throws Exception {
        call("/api/imports", productId, warehouseId, "\"abc\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }
}