package com.xytang.common.core.response;

/**
 * 错误码接口：统一业务错误码的契约。
 *
 * <p>由 {@link BizCode} 枚举实现，也可由业务方自定义实现以扩展错误码。
 * core 为纯 POJO 模块，HTTP 状态码以数值表示，由使用方映射为框架类型。
 */
public interface ErrorCode {

    /**
     * 业务状态码（200 成功；1xxxx 参数 / 2xxxx 用户权限 / 3xxxx 业务 / 4xxxx 第三方 / 5xxxx 系统）。
     *
     * @return 业务状态码
     */
    Integer getCode();

    /**
     * 用户可读的友好文案（直接展示给前端）。
     *
     * @return 友好文案
     */
    String getUserMessage();

    /**
     * 开发者详情模板（支持 {@code {}} 占位符，仅开发/测试环境经日志暴露）。
     *
     * @return 开发者详情模板
     */
    String getDevMessage();

    /**
     * 对应的 HTTP 状态码数值。
     *
     * @return HTTP 状态码数值
     */
    int getHttpStatus();
}