package com.example.supplychain.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.supplychain.dto.TransferRequest;
import com.example.supplychain.entity.Inventory;
import com.example.supplychain.entity.StockTransfer;
import com.example.supplychain.entity.TransferDetail;
import com.example.supplychain.exception.TransferBusinessException;
import com.example.supplychain.repository.InventoryRepository;
import com.example.supplychain.repository.ProductRepository;
import com.example.supplychain.repository.StockTransferRepository;
import com.example.supplychain.repository.TransferDetailRepository;
import com.example.supplychain.repository.WarehouseRepository;

@Service
public class TransferService {

    private final StockTransferRepository transferRepository;
    private final TransferDetailRepository detailRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public TransferService(
            StockTransferRepository transferRepository,
            TransferDetailRepository detailRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository) {

        this.transferRepository = transferRepository;
        this.detailRepository = detailRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public StockTransfer createTransfer(TransferRequest request) {
        Long source = request.getFromWarehouseId();
        Long target = request.getToWarehouseId();

        if (source == null || target == null || source.equals(target)) {
            throw error("Kho nguon va kho dich phai khac nhau");
        }

        if (!warehouseRepository.existsById(source)
                || !warehouseRepository.existsById(target)) {
            throw error("Kho khong ton tai");
        }

        if (request.getProductId() == null
                || !productRepository.existsById(request.getProductId())) {
            throw error("San pham khong ton tai");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw error("So luong phai lon hon 0");
        }

        if (request.getTransferCode() == null
                || request.getTransferCode().isBlank()) {
            throw error("Ma phieu khong duoc de trong");
        }

        String code = request.getTransferCode().trim();

        if (transferRepository.existsByTransferCode(code)) {
            throw error("Ma phieu da ton tai");
        }

        StockTransfer transfer = new StockTransfer();
        transfer.setTransferCode(code);
        transfer.setFromWarehouseId(source);
        transfer.setToWarehouseId(target);
        transfer.setTransferDate(LocalDate.now());
        transfer.setStatus("DRAFT");

        transferRepository.save(transfer);

        TransferDetail detail = new TransferDetail();
        detail.setTransferId(transfer.getId());
        detail.setProductId(request.getProductId());
        detail.setQuantity(request.getQuantity());

        detailRepository.save(detail);

        return transfer;
    }

    @Transactional
    public StockTransfer completeTransfer(Long id) {
        StockTransfer transfer = lockDraft(id);

        Long sourceId = transfer.getFromWarehouseId();
        Long targetId = transfer.getToWarehouseId();

        if (sourceId == null || targetId == null
                || sourceId.equals(targetId)) {
            throw error("Kho nguon va kho dich khong hop le");
        }

        // Khoa cac kho theo thu tu co dinh de giam nguy co deadlock.
        lockWarehouse(Math.min(sourceId, targetId));
        lockWarehouse(Math.max(sourceId, targetId));

        List<TransferDetail> details =
                detailRepository.findByTransferIdOrderByProductIdAsc(id);

        if (details.size() != 1) {
            throw error("Phieu phai co dung mot chi tiet san pham");
        }

        TransferDetail detail = details.get(0);
        Integer quantity = detail.getQuantity();

        if (detail.getProductId() == null
                || quantity == null
                || quantity <= 0) {
            throw error("Chi tiet chuyen kho khong hop le");
        }

        if (!productRepository.existsById(detail.getProductId())) {
            throw error("San pham khong ton tai");
        }

        Inventory source = inventoryRepository.findForUpdate(
                sourceId,
                detail.getProductId()
        ).orElseThrow(() ->
                error("San pham chua co trong kho nguon")
        );

        if (source.getQuantity() == null
                || source.getQuantity() < quantity) {
            throw error("Kho nguon khong du hang de chuyen");
        }

        Inventory target = inventoryRepository.findForUpdate(
                targetId,
                detail.getProductId()
        ).orElse(null);

        if (target == null) {
            target = new Inventory();
            target.setWarehouseId(targetId);
            target.setProductId(detail.getProductId());
            target.setQuantity(0);
            target.setMinimumQuantity(0);
        }

        if (target.getQuantity() == null
                || target.getQuantity() < 0) {
            throw error("Ton kho dich khong hop le");
        }

        int updatedTarget;

        try {
            updatedTarget = Math.addExact(
                    target.getQuantity(),
                    quantity
            );
        } catch (ArithmeticException exception) {
            throw error("So luong ton kho vuot gioi han cho phep");
        }

        source.setQuantity(source.getQuantity() - quantity);
        target.setQuantity(updatedTarget);

        inventoryRepository.save(source);
        inventoryRepository.save(target);

        transfer.setStatus("COMPLETED");

        return transferRepository.save(transfer);
    }

    @Transactional
    public StockTransfer cancelTransfer(Long id) {
        StockTransfer transfer = lockDraft(id);

        transfer.setStatus("CANCELLED");

        return transferRepository.save(transfer);
    }

    private StockTransfer lockDraft(Long id) {
        if (id == null || id <= 0) {
            throw error("ID phieu khong hop le");
        }

        StockTransfer transfer = transferRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        error("Khong tim thay phieu chuyen kho")
                );

        if (!"DRAFT".equals(transfer.getStatus())) {
            throw error("Chi phieu DRAFT moi duoc hoan tat hoac huy");
        }

        return transfer;
    }

    private void lockWarehouse(Long id) {
        warehouseRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        error("Kho khong ton tai")
                );
    }

    private TransferBusinessException error(String message) {
        return new TransferBusinessException(message);
    }
}