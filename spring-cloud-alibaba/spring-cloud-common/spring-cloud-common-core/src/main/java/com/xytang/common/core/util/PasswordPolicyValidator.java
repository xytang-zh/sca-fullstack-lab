package com.xytang.common.core.util;

import com.xytang.common.core.exception.BusinessException;
import com.xytang.common.core.response.BizCode;

import java.util.regex.Pattern;

/**
 * 密码策略校验器（OWASP 2024 强密码基线）。
 *
 * <p>规则：
 * <ul>
 *   <li>长度 ≥ 8</li>
 *   <li>四类字符（小写字母 / 大写字母 / 数字 / 符号）至少出现 3 类</li>
 *   <li>禁止空白字符（空格、Tab、换行）</li>
 * </ul>
 *
 * <p>校验失败抛 {@link BusinessException}，bizCode 为 {@link BizCode#PASSWORD_WEAK}，
 * HTTP 码 400。开发者可通过 {@link DevMessageHolder} 注入具体失败原因供 dev 环境诊断。
 *
 * <p>使用示例：
 * <pre>{@code
 * PasswordPolicyValidator.validate(newPwd, "长度不足", "缺少大写字母");
 * }</pre>
 */
public final class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MIN_CATEGORIES = 3;

    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL = Pattern.compile("[^a-zA-Z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s");

    private PasswordPolicyValidator() {
    }

    /**
     * 校验密码强度，失败抛 BusinessException(PASSWORD_WEAK)。
     *
     * @param password 待校验密码明文
     * @throws BusinessException 密码不符合策略
     */
    public static void validate(String password) {
        validate(password, null);
    }

    /**
     * 校验密码强度，失败抛 BusinessException(PASSWORD_WEAK)，
     * 同时通过 DevMessageHolder 注入具体原因（dev 环境填充到 R&lt;T&gt;.devMessage）。
     *
     * @param password   待校验密码明文
     * @param devMessage 具体失败原因（可空），dev 环境回填到响应体
     * @throws BusinessException 密码不符合策略
     */
    public static void validate(String password, String devMessage) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new BusinessException(BizCode.PASSWORD_WEAK,
                "密码长度必须 ≥ " + MIN_LENGTH + " 位",
                devMessage != null ? devMessage : "password too short");
        }
        if (WHITESPACE.matcher(password).find()) {
            throw new BusinessException(BizCode.PASSWORD_WEAK,
                "密码不能包含空白字符",
                devMessage != null ? devMessage : "password contains whitespace");
        }
        int categories = countCategories(password);
        if (categories < MIN_CATEGORIES) {
            throw new BusinessException(BizCode.PASSWORD_WEAK,
                "密码必须包含大小写字母、数字、符号中的至少 3 类",
                devMessage != null ? devMessage : "only " + categories + " categories present");
        }
    }

    private static int countCategories(String password) {
        int count = 0;
        if (LOWER.matcher(password).find()) {
            count++;
        }
        if (UPPER.matcher(password).find()) {
            count++;
        }
        if (DIGIT.matcher(password).find()) {
            count++;
        }
        if (SYMBOL.matcher(password).find()) {
            count++;
        }
        return count;
    }
}
