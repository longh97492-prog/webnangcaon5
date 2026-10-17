package com.example.supplychain.exception;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400: dữ liệu DTO không hợp lệ
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                req
        );
    }

    // 400: body JSON sai định dạng hoặc sai kiểu
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "Body JSON sai định dạng hoặc sai kiểu dữ liệu",
                req
        );
<<<<<<< HEAD
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameter(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest req) {

        String message = "Tham số '" + ex.getName()
                + "' không đúng kiểu dữ liệu";

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                message,
                req
        );
=======
>>>>>>> group/develop
    }

    // 400: tham số trên đường dẫn hoặc query sai kiểu
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Tham số " + ex.getName() + " không đúng kiểu dữ liệu",
                req
        );
    }

    // 400: kiểm tra đầu vào trong service
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                ex.getMessage(),
                req
        );
    }

    // 404: không tìm thấy tài nguyên
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                req
        );
<<<<<<< HEAD
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Không tìm thấy tài nguyên được yêu cầu",
                req
        );
=======
>>>>>>> group/develop
    }

    // 409: xung đột nghiệp vụ
    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            BusinessConflictException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.CONFLICT,
                "BUSINESS_CONFLICT",
                ex.getMessage(),
                req
        );
    }

    // 403: không đủ quyền
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            AccessDeniedException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Không đủ quyền thực hiện thao tác này",
                req
        );
    }

    // 422: lỗi nghiệp vụ chuyển kho
    @ExceptionHandler(TransferBusinessException.class)
    public ResponseEntity<ErrorResponse> handleTransferBusiness(
            TransferBusinessException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "TRANSFER_BUSINESS_ERROR",
                ex.getMessage(),
                req
        );
    }

    // 404: không tìm thấy đường dẫn hoặc tài nguyên
    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleMissingRoute(
            Exception ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Không tìm thấy đường dẫn hoặc tài nguyên",
                req
        );
    }

    // 500: lỗi ngoài dự kiến, không trả chi tiết nội bộ
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(
            Exception ex,
            HttpServletRequest req) {

<<<<<<< HEAD
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Lỗi hệ thống",
                req
=======
        String requestId = UUID.randomUUID().toString();

        // Chi tiết exception chỉ ghi ở log server.
        log.error(
                "requestId={} method={} path={} status=500 code=INTERNAL_ERROR",
                requestId,
                req.getMethod(),
                req.getRequestURI(),
                ex
        );

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Lỗi hệ thống",
                req,
                requestId
>>>>>>> group/develop
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest req) {

        String requestId = UUID.randomUUID().toString();

        log.warn(
                "requestId={} method={} path={} status={} code={}",
                requestId,
                req.getMethod(),
                req.getRequestURI(),
                status.value(),
                code
        );

        return response(status, code, message, req, requestId);
    }

    private ResponseEntity<ErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest req,
            String requestId) {

        return ResponseEntity.status(status)
                .header("X-Request-ID", requestId)
                .body(new ErrorResponse(
                        code,
                        message,
                        LocalDateTime.now(),
                        req.getRequestURI(),
                        requestId
                ));
    }
}