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

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400: dß╗» liß╗çu DTO kh├┤ng hß╗úp lß╗ç
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return build(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                message,
                req
        );
    }

    // 400: body JSON sai ─æß╗ïnh dß║íng hoß║╖c sai kiß╗âu
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

    // 400: tham sß╗æ tr├¬n ─æ╞░ß╗¥ng dß║½n hoß║╖c query sai kiß╗âu
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest req) {

        return build(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Tham sß╗æ " + ex.getName() + " kh├┤ng ─æ├║ng kiß╗âu dß╗» liß╗çu",
                req
        );
    }

    // 400: kiß╗âm tra ─æß║ºu v├áo trong service
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

    // 404: kh├┤ng t├¼m thß║Ñy t├ái nguy├¬n
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

    // 409: xung ─æß╗Öt nghiß╗çp vß╗Ñ
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

    // 403: kh├┤ng ─æß╗º quyß╗ün
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

    // 422: lß╗ùi nghiß╗çp vß╗Ñ chuyß╗ân kho
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

    // 404: kh├┤ng t├¼m thß║Ñy ─æ╞░ß╗¥ng dß║½n hoß║╖c t├ái nguy├¬n
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
                "Kh├┤ng t├¼m thß║Ñy ─æ╞░ß╗¥ng dß║½n hoß║╖c t├ái nguy├¬n",
                req
        );
    }

    // 500: lß╗ùi ngo├ái dß╗▒ kiß║┐n, kh├┤ng trß║ú chi tiß║┐t nß╗Öi bß╗Ö
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(
            Exception ex,
            HttpServletRequest req) {

        String requestId = UUID.randomUUID().toString();

        // Chi tiß║┐t exception chß╗ë ghi ß╗ƒ log server.
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
                "Lß╗ùi hß╗ç thß╗æng",
                req,
                requestId
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
