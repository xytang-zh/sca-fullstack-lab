package com.xytang.common.core.response;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class BizCodeTest {

    private static final int CODE_SUCCESS = 200;
    private static final int SEGMENT_BASE = 10000;
    private static final int MAX_SEGMENT = 5;
    private static final int AUTH_SEGMENT_LO = 21000;
    private static final int AUTH_SEGMENT_HI = 22000;
    private static final int SYSTEM_SEGMENT_LO = 22000;
    private static final int SYSTEM_SEGMENT_HI = 23000;
    private static final int GATEWAY_SEGMENT_LO = 29000;
    private static final int GATEWAY_SEGMENT_HI = 30000;
    private static final int UNKNOWN_CODE = 99999;
    private static final int LOCK_MINUTES = 15;
    private static final int MIN_AUTH_COUNT = 7;
    private static final int MIN_SYSTEM_COUNT = 5;
    private static final int MIN_GATEWAY_COUNT = 2;

    private static final Set<Integer> ALLOWED_HTTP_CODES = Set.of(
            200, 400, 401, 403, 404, 405, 409, 423, 429, 500, 503, 504);

    @Test
    void shouldHaveAllowedHttpCode() {
        for (BizCode bc : BizCode.values()) {
            assertTrue(ALLOWED_HTTP_CODES.contains(bc.getHttpStatus()),
                    "BizCode " + bc.name() + " httpStatus=" + bc.getHttpStatus() + " 不在允许范围");
        }
    }

    @Test
    void shouldHaveNonBlankUserMessage() {
        for (BizCode bc : BizCode.values()) {
            assertNotNull(bc.getUserMessage(), "BizCode " + bc.name() + " userMessage 为 null");
            assertTrue(!bc.getUserMessage().isBlank(), "BizCode " + bc.name() + " userMessage 为空白");
        }
    }

    @Test
    void shouldHaveUniqueCodes() {
        Set<Integer> seen = new HashSet<>();
        for (BizCode bc : BizCode.values()) {
            if (!seen.add(bc.getCode())) {
                fail("BizCode " + bc.name() + " code '" + bc.getCode() + "' 重复");
            }
        }
    }

    @Test
    void shouldFollowSegmentConvention() {
        for (BizCode bc : BizCode.values()) {
            int code = bc.getCode();
            if (code == CODE_SUCCESS) {
                continue;
            }
            int segment = code / SEGMENT_BASE;
            assertTrue(segment >= 1 && segment <= MAX_SEGMENT,
                    "BizCode " + bc.name() + " 区段 " + segment + " 不在 1-5 范围");
        }
    }

    @Test
    void successShouldBe200() {
        assertEquals(CODE_SUCCESS, BizCode.SUCCESS.getCode());
        assertEquals(CODE_SUCCESS, BizCode.SUCCESS.getHttpStatus());
        assertEquals("操作成功", BizCode.SUCCESS.getUserMessage());
    }

    @Test
    void shouldLookupByCode() {
        for (BizCode bc : BizCode.values()) {
            assertTrue(BizCode.fromCode(bc.getCode()).isPresent());
            assertEquals(bc, BizCode.fromCode(bc.getCode()).orElseThrow());
        }
    }

    @Test
    void shouldReturnEmptyForUnknownCode() {
        assertTrue(BizCode.fromCode(UNKNOWN_CODE).isEmpty());
    }

    @Test
    void shouldFormatDevMessage() {
        String formatted = BizCode.AUTH_USER_LOCKED.formatDevMessage(LOCK_MINUTES);
        assertTrue(formatted.contains(String.valueOf(LOCK_MINUTES)), "格式化结果应包含占位符替换值");
        assertTrue(formatted.contains("userId"), "格式化结果应保留原有文本");
    }

    @Test
    void shouldHaveAuthAndSystemAndGatewaySegments() {
        long authCount = Arrays.stream(BizCode.values())
                .filter(b -> b.getCode() >= AUTH_SEGMENT_LO && b.getCode() < AUTH_SEGMENT_HI).count();
        long systemCount = Arrays.stream(BizCode.values())
                .filter(b -> b.getCode() >= SYSTEM_SEGMENT_LO && b.getCode() < SYSTEM_SEGMENT_HI).count();
        long gatewayCount = Arrays.stream(BizCode.values())
                .filter(b -> b.getCode() >= GATEWAY_SEGMENT_LO && b.getCode() < GATEWAY_SEGMENT_HI).count();
        assertTrue(authCount >= MIN_AUTH_COUNT, "auth 区段 BizCode 应不少于 7 个，实际 " + authCount);
        assertTrue(systemCount >= MIN_SYSTEM_COUNT, "system 区段 BizCode 应不少于 5 个，实际 " + systemCount);
        assertTrue(gatewayCount >= MIN_GATEWAY_COUNT, "网关区段 BizCode 应不少于 2 个，实际 " + gatewayCount);
    }
}