package com.xytang.common.web.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.xytang.common.core.exception.BusinessException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器（单一业务码体系）。
 *
 * <p>异常 → 业务码 → HTTP 状态码映射；message 始终为用户友好文案，
 * 开发详情仅通过日志输出，不写入响应体。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusiness(BusinessException e, HttpServletRequest req) {
        log.warn("[BizException] {} {} -> code={} msg={}",
                req.getMethod(), req.getRequestURI(), e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.valueOf(e.getHttpStatus()))
            .body(R.fail(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String message = BizCode.PARAM_ERROR.getUserMessage() + "：" + detail;
        log.warn("[ParamInvalid] {} {} -> {}", req.getMethod(), req.getRequestURI(), detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException e, HttpServletRequest req) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String message = BizCode.PARAM_ERROR.getUserMessage() + "：" + detail;
        log.warn("[BindInvalid] {} {} -> {}", req.getMethod(), req.getRequestURI(), detail);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParam(MissingServletRequestParameterException e,
                                                       HttpServletRequest req) {
        String message = BizCode.PARAM_MISSING.getUserMessage() + "：" + e.getParameterName();
        log.warn("[ParamMissing] {} {} -> {}", req.getMethod(), req.getRequestURI(), e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_MISSING, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                      HttpServletRequest req) {
        String message = BizCode.PARAM_TYPE_ERROR.getUserMessage() + "：" + e.getName();
        log.warn("[ParamTypeMismatch] {} {} -> {}", req.getMethod(), req.getRequestURI(), e.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_TYPE_ERROR, message));
    }

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> handleNotLogin(NotLoginException e, HttpServletRequest req) {
        log.warn("[NotLogin] {} {} -> type={}", req.getMethod(), req.getRequestURI(), e.getType());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(R.fail(BizCode.AUTH_TOKEN_MISSING));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnknown(Exception e, HttpServletRequest req) {
        log.error("[UnknownException] {} {} ", req.getMethod(), req.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(R.fail(BizCode.SYS_ERROR));
    }
}