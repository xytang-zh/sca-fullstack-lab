package com.xytang.common.core.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RTest {

    private static final int CODE_SUCCESS = 200;
    private static final int CODE_PARAM_ERROR = 10001;
    private static final int CODE_PASSWORD_ERROR = 21003;
    private static final int CODE_SYS_ERROR = 50000;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void okShouldReturnSuccessEnvelope() {
        R<String> r = R.ok("payload");
        assertEquals(CODE_SUCCESS, r.getCode());
        assertEquals("操作成功", r.getMessage());
        assertEquals("payload", r.getData());
        assertNotNull(r.getTimestamp());
        assertTrue(r.isSuccess());
    }

    @Test
    void okNoDataShouldHaveNullData() {
        R<Void> r = R.ok();
        assertEquals(CODE_SUCCESS, r.getCode());
        assertNull(r.getData());
    }

    @Test
    void okWithMessageShouldOverrideDefaultMessage() {
        R<String> r = R.ok("自定义消息", "data");
        assertEquals("自定义消息", r.getMessage());
        assertEquals("data", r.getData());
    }

    @Test
    void failByErrorCodeShouldUseBizCodeFromEnum() {
        R<Void> r = R.fail(BizCode.AUTH_PASSWORD_ERROR);
        assertEquals(CODE_PASSWORD_ERROR, r.getCode());
        assertEquals("用户名或密码错误", r.getMessage());
        assertNull(r.getData());
        assertFalse(r.isSuccess());
    }

    @Test
    void failByErrorCodeWithMessageShouldOverrideMessage() {
        R<Void> r = R.fail(BizCode.AUTH_PASSWORD_ERROR, "账号或密码错误");
        assertEquals("账号或密码错误", r.getMessage());
    }

    @Test
    void failByCodeMessageShouldSetAllFields() {
        R<Void> r = R.fail(CODE_SYS_ERROR, "系统繁忙");
        assertEquals(CODE_SYS_ERROR, r.getCode());
        assertEquals("系统繁忙", r.getMessage());
    }

    @Test
    void chainShouldFillTraceId() {
        R<Void> r = R.<Void>fail(BizCode.SYS_ERROR).traceId("abc-123");
        assertEquals("abc-123", r.getTraceId());
    }

    @Test
    void isSuccessShouldRequireCode200() {
        assertTrue(R.ok().isSuccess());
        assertFalse(R.fail(CODE_PARAM_ERROR, "参数错误").isSuccess());
        assertFalse(R.fail(CODE_SYS_ERROR, "系统错误").isSuccess());
    }

    @Test
    void shouldNotSerializeLegacyFieldsWhenNull() throws Exception {
        R<Void> r = R.fail(BizCode.SYS_ERROR);
        String json = MAPPER.writeValueAsString(r);
        JsonNode node = MAPPER.readTree(json);
        assertFalse(node.has("bizCode"), "bizCode 字段应移除");
        assertFalse(node.has("path"), "path 字段应移除");
        assertFalse(node.has("devMessage"), "devMessage 字段应移除");
        assertFalse(node.has("traceId"), "traceId 为 null 时不应序列化");
    }

    @Test
    void shouldSerializeTimestampAsLong() throws Exception {
        R<Void> r = R.ok();
        String json = MAPPER.writeValueAsString(r);
        JsonNode node = MAPPER.readTree(json);
        assertTrue(node.has("timestamp"));
        assertTrue(node.get("timestamp").isNumber(), "timestamp 应为 Long 毫秒数字");
    }
}