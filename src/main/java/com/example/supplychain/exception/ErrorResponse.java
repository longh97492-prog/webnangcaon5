package com.example.supplychain.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp,
        String path,
        String requestId) {
}