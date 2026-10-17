package com.example.supplychain.exception;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                            HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadBody(HttpMessageNotReadableException ex,
                                                       HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY",
                "Body JSON sai định dạng hoặc sai kiểu dữ liệu", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Tham số '" + ex.getName() + "' không đúng kiểu dữ liệu", req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), req);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                         HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleMissingRoute(Exception ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Không tìm thấy đường dẫn hoặc tài nguyên", req);
    }

    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(BusinessConflictException ex,
                                                        HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", ex.getMessage(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex,
                                                         HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Không đủ quyền thực hiện thao tác này", req);
    }

    @ExceptionHandler(TransferBusinessException.class)
    public ResponseEntity<ErrorResponse> handleTransferBusiness(TransferBusinessException ex,
                                                                  HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "TRANSFER_BUSINESS_ERROR", ex.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest req) {
        String requestId = UUID.randomUUID().toString();
        log.error("requestId={} method={} path={} status=500 code=INTERNAL_ERROR",
                requestId, req.getMethod(), req.getRequestURI(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Lỗi hệ thống", req, requestId);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code,
                                                String message, HttpServletRequest req) {
        String requestId = UUID.randomUUID().toString();
        log.warn("requestId={} method={} path={} status={} code={}",
                requestId, req.getMethod(), req.getRequestURI(), status.value(), code);
        return response(status, code, message, req, requestId);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code,
                                                   String message, HttpServletRequest req,
                                                   String requestId) {
        return ResponseEntity.status(status)
                .header("X-Request-ID", requestId)
                .body(new ErrorResponse(code, message, LocalDateTime.now(),
                        req.getRequestURI(), requestId));
    }
}
