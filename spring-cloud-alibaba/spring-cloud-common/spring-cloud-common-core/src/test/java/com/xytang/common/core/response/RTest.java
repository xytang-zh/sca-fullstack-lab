package com.xytang.common.core.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RTest {

    private static final int HTTP_OK = 200;
    private static final int HTTP_BAD_REQUEST = 400;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int HTTP_INTERNAL_ERROR = 500;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void okShouldReturnSuccessEnvelope() {
        R<String> r = R.ok("payload");
        assertEquals(HTTP_OK, r.getCode());
        assertEquals("00000", r.getBizCode());
        assertEquals("操作成功", r.getMessage());
        assertEquals("payload", r.getData());
        assertNotNull(r.getTimestamp());
        assertTrue(r.isSuccess());
    }

    @Test
    void okNoDataShouldHaveNullData() {
        R<Void> r = R.ok();
        assertEquals(HTTP_OK, r.getCode());
        assertEquals("00000", r.getBizCode());
        assertNull(r.getData());
    }

    @Test
    void okWithMessageShouldOverrideDefaultMessage() {
        R<String> r = R.ok("自定义消息", "data");
        assertEquals("自定义消息", r.getMessage());
        assertEquals("data", r.getData());
    }

    @Test
    void failByBizCodeShouldUseHttpAndBizCodeFromEnum() {
        R<Void> r = R.fail(BizCode.AUTH_PASSWORD_ERROR);
        assertEquals(HTTP_BAD_REQUEST, r.getCode());
        assertEquals("01105", r.getBizCode());
        assertEquals("用户名或密码错误", r.getMessage());
        assertNull(r.getData());
        assertFalse(r.isSuccess());
    }

    @Test
    void failByBizCodeWithMessageShouldOverrideMessage() {
        R<Void> r = R.fail(BizCode.AUTH_PASSWORD_ERROR, "账号或密码错误");
        assertEquals("01105", r.getBizCode());
        assertEquals("账号或密码错误", r.getMessage());
    }

    @Test
    void failByHttpBizCodeMessageShouldSetAllFields() {
        R<Void> r = R.fail(HTTP_TOO_MANY_REQUESTS, "00201", "限流");
        assertEquals(HTTP_TOO_MANY_REQUESTS, r.getCode());
        assertEquals("00201", r.getBizCode());
        assertEquals("限流", r.getMessage());
    }

    @Test
    void chainShouldFillTraceIdPathDevMessage() {
        R<Void> r = R.<Void>fail(BizCode.SYS_ERROR)
                .traceId("abc-123")
                .path("/api/test")
                .devMessage("NullPointer at line 42");
        assertEquals("abc-123", r.getTraceId());
        assertEquals("/api/test", r.getPath());
        assertEquals("NullPointer at line 42", r.getDevMessage());
    }

    @Test
    void isSuccessShouldRequireBothHttpAndBizCodeSuccess() {
        R<Void> r1 = R.ok();
        assertTrue(r1.isSuccess());

        R<Void> r2 = R.fail(HTTP_OK, "00101", "参数错误");
        assertFalse(r2.isSuccess());

        R<Void> r3 = R.fail(HTTP_INTERNAL_ERROR, "00000", "意外");
        assertFalse(r3.isSuccess());
    }

    @Test
    void shouldNotSerializeNullDevMessageInProd() throws Exception {
        R<Void> r = R.fail(BizCode.AUTH_PASSWORD_ERROR);
        assertNull(r.getDevMessage());
        String json = MAPPER.writeValueAsString(r);
        JsonNode node = MAPPER.readTree(json);
        assertFalse(node.has("devMessage"), "devMessage 为 null 时不应序列化");
    }

    @Test
    void shouldSerializeDevMessageWhenPresent() throws Exception {
        R<Void> r = R.<Void>fail(BizCode.AUTH_PASSWORD_ERROR)
                .devMessage("Argon2id matches 返回 false");
        String json = MAPPER.writeValueAsString(r);
        JsonNode node = MAPPER.readTree(json);
        assertTrue(node.has("devMessage"));
        assertEquals("Argon2id matches 返回 false", node.get("devMessage").asText());
    }

    @Test
    void shouldNotSerializeNullPathAndTraceId() throws Exception {
        R<Void> r = R.fail(BizCode.SYS_ERROR);
        String json = MAPPER.writeValueAsString(r);
        JsonNode node = MAPPER.readTree(json);
        assertFalse(node.has("path"), "path 为 null 时不应序列化");
        assertFalse(node.has("traceId"), "traceId 为 null 时不应序列化");
    }

    @Test
    void shouldSerializeTimestampAsIso8601() throws Exception {
        R<Void> r = R.ok();
        r.setTimestamp(Instant.parse("2026-07-30T14:55:54.538Z"));
        String json = MAPPER.writeValueAsString(r);
        JsonNode node = MAPPER.readTree(json);
        assertTrue(node.has("timestamp"));
        assertTrue(node.get("timestamp").isTextual());
        assertEquals("2026-07-30T14:55:54.538Z", node.get("timestamp").asText());
    }
}
