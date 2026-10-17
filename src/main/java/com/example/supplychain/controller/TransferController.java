package com.example.supplychain.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.supplychain.dto.TransferRequest;
import com.example.supplychain.entity.StockTransfer;
import com.example.supplychain.service.TransferService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<StockTransfer> createTransfer(
            @Valid @RequestBody TransferRequest request) {

        StockTransfer transfer = transferService.createTransfer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(transfer);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<StockTransfer> completeTransfer(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(transferService.completeTransfer(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<StockTransfer> cancelTransfer(
            @PathVariable("id") Long id) {

        return ResponseEntity.ok(transferService.cancelTransfer(id));
    }
}