package com.staffboard.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return buildResponse(ex.getErrorCode().code(), ex.getMessage(),
            ex.getErrorCode().httpStatus().value());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        Map<String, Object> body = new HashMap<>();
        body.put("code", ErrorCode.BAD_REQUEST.code());
        body.put("message", "参数校验失败");
        body.put("data", errors);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return buildResponse(ErrorCode.INTERNAL_ERROR.code(), "服务器内部错误", 500);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(int code, String msg, int httpStatus) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", msg);
        body.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.status(httpStatus).body(body);
    }
}
