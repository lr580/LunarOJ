package com.lunaroj.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.fail(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        return ApiResponse.fail(ErrorCode.VALIDATION_ERROR, resolveValidationMessage(ex.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        return ApiResponse.fail(ErrorCode.VALIDATION_ERROR, resolveValidationMessage(ex.getBindingResult()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
        return ApiResponse.fail(ErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleHttpMessageNotReadableException() {
        return ApiResponse.fail(ErrorCode.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("未捕获异常", ex);
        return ApiResponse.fail(ErrorCode.INTERNAL_ERROR);
    }

    private String resolveValidationMessage(BindingResult bindingResult) {
        return bindingResult.getFieldError() == null
                ? ErrorCode.VALIDATION_ERROR.getMessage()
                : bindingResult.getFieldError().getDefaultMessage();
    }
}
