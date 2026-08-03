package com.xytang.common.web.handler;

import com.xytang.common.core.exception.BusinessException;
import com.xytang.common.core.response.BizCode;
import com.xytang.common.core.response.DevMessageHolder;
import com.xytang.common.core.response.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

/**
 * 全局异常处理器（双层响应码体系：HTTP 状态码 + 5 位 bizCode）。
 *
 * <p>异常 → BizCode → HTTP 状态码映射；dev 环境填充 DevMessageHolder 供
 * RResponseAdvice 在响应阶段回填到 R&lt;T&gt;.devMessage。
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Environment environment;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusiness(BusinessException e, HttpServletRequest req) {
        log.warn("[BizException] {} {} -> bizCode={} msg={}",
                req.getMethod(), req.getRequestURI(), e.getBizCode().code(), e.getMessage());
        fillDevMessageIfNeeded(e);
        HttpStatus status = HttpStatus.valueOf(e.getBizCode().httpCode());
        return ResponseEntity.status(status).body(R.fail(e.getBizCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String message = BizCode.PARAM_ERROR.message() + "：" + detail;
        log.warn("[ParamInvalid] {} {} -> {}", req.getMethod(), req.getRequestURI(), detail);
        fillDevMessageIfNeeded(e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException e, HttpServletRequest req) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        String message = BizCode.PARAM_ERROR.message() + "：" + detail;
        log.warn("[BindInvalid] {} {} -> {}", req.getMethod(), req.getRequestURI(), detail);
        fillDevMessageIfNeeded(e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_ERROR, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParam(MissingServletRequestParameterException e,
                                                       HttpServletRequest req) {
        String message = BizCode.PARAM_MISSING.message() + "：" + e.getParameterName();
        log.warn("[ParamMissing] {} {} -> {}", req.getMethod(), req.getRequestURI(), e.getParameterName());
        fillDevMessageIfNeeded(e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_MISSING, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                      HttpServletRequest req) {
        String message = BizCode.PARAM_TYPE_ERROR.message() + "：" + e.getName();
        log.warn("[ParamTypeMismatch] {} {} -> {}", req.getMethod(), req.getRequestURI(), e.getName());
        fillDevMessageIfNeeded(e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_TYPE_ERROR, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnknown(Exception e, HttpServletRequest req) {
        log.error("[UnknownException] {} {} ", req.getMethod(), req.getRequestURI(), e);
        fillDevMessageIfNeeded(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(R.fail(BizCode.SYS_ERROR));
    }

    private void fillDevMessageIfNeeded(Exception e) {
        if (!environment.acceptsProfiles("dev")) {
            return;
        }
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        DevMessageHolder.set(sw.toString());
    }
}
