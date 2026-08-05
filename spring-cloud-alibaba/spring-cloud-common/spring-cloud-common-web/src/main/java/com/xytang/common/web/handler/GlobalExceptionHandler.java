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

    /**
     * 业务异常处理：按其自带的错误码与 HTTP 状态码返回，message 不暴露内部细节。
     *
     * @param e   业务异常（含 code/message/httpStatus）
     * @param req 当前请求（用于日志定位）
     * @return 统一失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusiness(BusinessException e, HttpServletRequest req) {
        log.warn("[BizException] {} {} -> code={} msg={}",
                req.getMethod(), req.getRequestURI(), e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.valueOf(e.getHttpStatus()))
            .body(R.fail(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 请求体参数校验失败（@Valid @RequestBody DTO），拼接字段错误明细后返回 400。
     *
     * @param e   处理方法参数校验异常
     * @param req 当前请求（用于日志定位）
     * @return 400 参数错误响应
     */
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

    /**
     * 表单绑定校验失败（非 @RequestBody 场景），处理逻辑同参数校验。
     *
     * @param e   表单绑定异常
     * @param req 当前请求（用于日志定位）
     * @return 400 参数错误响应
     */
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

    /**
     * 缺少必要请求参数（@RequestParam 未传）。
     *
     * @param e   缺少参数异常
     * @param req 当前请求（用于日志定位）
     * @return 400 缺少参数响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Void>> handleMissingParam(MissingServletRequestParameterException e,
                                                       HttpServletRequest req) {
        String message = BizCode.PARAM_MISSING.getUserMessage() + "：" + e.getParameterName();
        log.warn("[ParamMissing] {} {} -> {}", req.getMethod(), req.getRequestURI(), e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_MISSING, message));
    }

    /**
     * 参数类型不匹配（如把字符串传给 Long 入参）。
     *
     * @param e   类型不匹配异常
     * @param req 当前请求（用于日志定位）
     * @return 400 参数类型错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                      HttpServletRequest req) {
        String message = BizCode.PARAM_TYPE_ERROR.getUserMessage() + "：" + e.getName();
        log.warn("[ParamTypeMismatch] {} {} -> {}", req.getMethod(), req.getRequestURI(), e.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.fail(BizCode.PARAM_TYPE_ERROR, message));
    }

    /**
     * Sa-Token 未登录异常：统一返回 401 与"未登录"文案，前端据此跳转登录页。
     *
     * @param e   未登录异常
     * @param req 当前请求（用于日志定位）
     * @return 401 未登录响应
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> handleNotLogin(NotLoginException e, HttpServletRequest req) {
        log.warn("[NotLogin] {} {} -> type={}", req.getMethod(), req.getRequestURI(), e.getType());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(R.fail(BizCode.AUTH_TOKEN_MISSING));
    }

    /**
     * 未预期异常兜底：记录完整堆栈（含方法/URI），对外只返回通用系统错误，避免泄露内部信息。
     *
     * @param e   未预期异常
     * @param req 当前请求（用于日志定位）
     * @return 500 系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnknown(Exception e, HttpServletRequest req) {
        log.error("[UnknownException] {} {} ", req.getMethod(), req.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(R.fail(BizCode.SYS_ERROR));
    }
}