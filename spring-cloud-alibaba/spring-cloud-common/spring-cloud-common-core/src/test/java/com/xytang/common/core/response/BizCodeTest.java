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

    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{5}$");
    private static final Set<Integer> ALLOWED_HTTP_CODES = Set.of(
        200, 400, 401, 403, 404, 405, 409, 422, 423, 429, 500, 503, 504);

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
            String moduleCode = bc.code().substring(0, 2);
            String errorCategory = bc.code().substring(2, 3);
            int moduleNo = Integer.parseInt(moduleCode);
            int categoryNo = Integer.parseInt(errorCategory);
            assertTrue(moduleNo == 0 || moduleNo == 1 || moduleNo == 2 || moduleNo == 99
                || (moduleNo >= 3 && moduleNo <= 12),
                "BizCode " + bc.name() + " 模块号 " + moduleNo + " 不在允许范围");
            assertTrue(categoryNo >= 0 && categoryNo <= 5,
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
        assertEquals(200, BizCode.SUCCESS.httpCode());
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
        assertTrue(coreCount >= 11, "通用模块 BizCode 应不少于 11 个，实际 " + coreCount);
        assertTrue(authCount >= 7, "auth 模块 BizCode 应不少于 7 个，实际 " + authCount);
        assertTrue(systemCount >= 5, "system 模块 BizCode 应不少于 5 个，实际 " + systemCount);
        assertTrue(gatewayCount >= 5, "gateway 模块 BizCode 应不少于 5 个，实际 " + gatewayCount);
    }

    @Test
    void shouldFormatPlaceholderMessage() {
        String formatted = BizCode.AUTH_USER_LOCKED.format(15);
        assertTrue(formatted.contains("15"), "格式化结果应包含占位符替换值");
        assertTrue(formatted.contains("分钟"), "格式化结果应保留原有文本");
    }
}
