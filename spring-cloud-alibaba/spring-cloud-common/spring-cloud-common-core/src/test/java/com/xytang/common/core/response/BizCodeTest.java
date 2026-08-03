package com.xytang.common.core.response;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BizCodeTest {

    private static final int HTTP_OK = 200;
    private static final int MODULE_CODE_LENGTH = 2;
    private static final int CATEGORY_CODE_LENGTH = 1;
    private static final int GATEWAY_MODULE_NO = 99;
    private static final int MIN_MODULE_NO = 3;
    private static final int MAX_MODULE_NO = 12;
    private static final int MAX_CATEGORY_NO = 5;
    private static final int MIN_CORE_COUNT = 11;
    private static final int MIN_AUTH_COUNT = 7;
    private static final int MIN_SYSTEM_COUNT = 5;
    private static final int MIN_GATEWAY_COUNT = 5;
    private static final int LOCK_MINUTES = 15;

    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{5}$");
    private static final Set<Integer> ALLOWED_HTTP_CODES = Set.of(
            200, 400, 401, 403, 404, 405, 409, 422, 423, 429, 500, 503, 504);
    private static final Set<Integer> ALLOWED_MODULE_NOS = Set.of(0, 1, 2, GATEWAY_MODULE_NO);

    @Test
    void shouldHaveFiveDigitStringCode() {
        for (BizCode bc : BizCode.values()) {
            assertTrue(CODE_PATTERN.matcher(bc.code()).matches(),
                    "BizCode " + bc.name() + " code '" + bc.code() + "' 不符合 5 位数字字符串格式");
        }
    }

    @Test
    void shouldHaveAllowedHttpCode() {
        for (BizCode bc : BizCode.values()) {
            assertTrue(ALLOWED_HTTP_CODES.contains(bc.httpCode()),
                    "BizCode " + bc.name() + " httpCode=" + bc.httpCode() + " 不在允许范围");
        }
    }

    @Test
    void shouldHaveNonBlankMessage() {
        for (BizCode bc : BizCode.values()) {
            assertNotNull(bc.message(), "BizCode " + bc.name() + " message 为 null");
            assertTrue(!bc.message().isBlank(), "BizCode " + bc.name() + " message 为空白");
        }
    }

    @Test
    void shouldHaveUniqueCodes() {
        Set<String> seen = new HashSet<>();
        for (BizCode bc : BizCode.values()) {
            if (!seen.add(bc.code())) {
                fail("BizCode " + bc.name() + " code '" + bc.code() + "' 重复");
            }
        }
    }

    @Test
    void shouldFollowModuleCodeConvention() {
        for (BizCode bc : BizCode.values()) {
            String moduleCode = bc.code().substring(0, MODULE_CODE_LENGTH);
            String errorCategory = bc.code().substring(MODULE_CODE_LENGTH,
                    MODULE_CODE_LENGTH + CATEGORY_CODE_LENGTH);
            int moduleNo = Integer.parseInt(moduleCode);
            int categoryNo = Integer.parseInt(errorCategory);
            assertTrue(ALLOWED_MODULE_NOS.contains(moduleNo)
                    || (moduleNo >= MIN_MODULE_NO && moduleNo <= MAX_MODULE_NO),
                    "BizCode " + bc.name() + " 模块号 " + moduleNo + " 不在允许范围");
            assertTrue(categoryNo >= 0 && categoryNo <= MAX_CATEGORY_NO,
                    "BizCode " + bc.name() + " 错误类别 " + categoryNo + " 不在 0-5 范围");
        }
    }

    @Test
    void shouldLookupByCode() {
        for (BizCode bc : BizCode.values()) {
            assertTrue(BizCode.fromCode(bc.code()).isPresent(),
                    "BizCode.fromCode(" + bc.code() + ") 返回 empty");
            assertEquals(bc, BizCode.fromCode(bc.code()).orElseThrow());
        }
    }

    @Test
    void shouldReturnEmptyForUnknownCode() {
        assertTrue(BizCode.fromCode("99999").isEmpty());
        assertTrue(BizCode.fromCode(null).isEmpty());
        assertTrue(BizCode.fromCode("").isEmpty());
    }

    @Test
    void shouldSuccessCodeBe00000WithHttp200() {
        assertEquals("00000", BizCode.SUCCESS.code());
        assertEquals(HTTP_OK, BizCode.SUCCESS.httpCode());
        assertEquals("操作成功", BizCode.SUCCESS.message());
    }

    @Test
    void shouldHaveCoreAuthSystemGatewayModules() {
        long coreCount = Arrays.stream(BizCode.values())
                .filter(b -> b.code().startsWith("00")).count();
        long authCount = Arrays.stream(BizCode.values())
                .filter(b -> b.code().startsWith("01")).count();
        long systemCount = Arrays.stream(BizCode.values())
                .filter(b -> b.code().startsWith("02")).count();
        long gatewayCount = Arrays.stream(BizCode.values())
                .filter(b -> b.code().startsWith("99")).count();
        assertTrue(coreCount >= MIN_CORE_COUNT, "通用模块 BizCode 应不少于 11 个，实际 " + coreCount);
        assertTrue(authCount >= MIN_AUTH_COUNT, "auth 模块 BizCode 应不少于 7 个，实际 " + authCount);
        assertTrue(systemCount >= MIN_SYSTEM_COUNT, "system 模块 BizCode 应不少于 5 个，实际 " + systemCount);
        assertTrue(gatewayCount >= MIN_GATEWAY_COUNT, "gateway 模块 BizCode 应不少于 5 个，实际 " + gatewayCount);
    }

    @Test
    void shouldFormatPlaceholderMessage() {
        String formatted = BizCode.AUTH_USER_LOCKED.format(LOCK_MINUTES);
        assertTrue(formatted.contains("15"), "格式化结果应包含占位符替换值");
        assertTrue(formatted.contains("分钟"), "格式化结果应保留原有文本");
    }
}
