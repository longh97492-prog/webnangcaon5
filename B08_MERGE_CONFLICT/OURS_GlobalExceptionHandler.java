package com.example.supplychain.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadBody(
            HttpMessageNotReadableException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "Body JSON sai ─æß╗ïnh dß║íng hoß║╖c sai kiß╗âu dß╗» liß╗çu",
                req
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameter(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest req) {

        String message = "Tham sß╗æ '" + ex.getName()
                + "' kh├┤ng ─æ├║ng kiß╗âu dß╗» liß╗çu";

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                message,
                req
        );
    }

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
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Kh├┤ng t├¼m thß║Ñy t├ái nguy├¬n ─æ╞░ß╗úc y├¬u cß║ºu",
                req
        );
    }

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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            AccessDeniedException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "Kh├┤ng ─æß╗º quyß╗ün thß╗▒c hiß╗çn thao t├íc n├áy",
                req
        );
    }

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(
            Exception ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Lß╗ùi hß╗ç thß╗æng",
                req
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest req) {

        return ResponseEntity.status(status).body(
                new ErrorResponse(
                        code,
                        message,
                        LocalDateTime.now(),
                        req.getRequestURI()
                )
        );
    }
}
