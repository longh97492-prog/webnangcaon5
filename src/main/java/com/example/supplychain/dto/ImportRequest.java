package com.example.supplychain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportRequest {

    @NotBlank(message = "Ma phieu nhap khong duoc de trong")
    private String receiptCode;

    @NotNull(message = "productId la bat buoc")
    private Long productId;

    @NotNull(message = "warehouseId la bat buoc")
    private Long warehouseId;

    private Long supplierId;

    private Long userId;

    @NotNull(message = "quantity la bat buoc")
    @Positive(message = "So luong nhap phai lon hon 0")
    private Integer quantity;

    public ImportRequest() {
    }

    public ImportRequest(
            String receiptCode,
            Long productId,
            Long warehouseId,
            Long supplierId,
            Long userId,
            Integer quantity) {

        this.receiptCode = receiptCode;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.supplierId = supplierId;
        this.userId = userId;
        this.quantity = quantity;
    }
}