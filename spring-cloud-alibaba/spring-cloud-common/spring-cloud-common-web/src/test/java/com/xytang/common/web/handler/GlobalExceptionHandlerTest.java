package com.xytang.common.web.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xytang.common.core.exception.BusinessException;
import com.xytang.common.core.response.BizCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理器测试：验证错误码 → HTTP 状态码映射与响应体字段。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>BusinessException → 对应 HTTP 码 + 业务码</li>
 *   <li>MethodArgumentNotValidException → 400 + 10001</li>
 *   <li>MissingServletRequestParameterException → 400 + 10002</li>
 *   <li>MethodArgumentTypeMismatchException → 400 + 10003</li>
 *   <li>兜底 Exception → 500 + 50000</li>
 * </ul>
 */
class GlobalExceptionHandlerTest {

    private static final int CODE_PASSWORD_ERROR = 21003;
    private static final int CODE_PARAM_ERROR = 10001;
    private static final int CODE_PARAM_MISSING = 10002;
    private static final int CODE_PARAM_TYPE_ERROR = 10003;
    private static final int CODE_SYS_ERROR = 50000;

    private final MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    GlobalExceptionHandlerTest() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        this.mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void businessExceptionShouldMapToErrorCodeHttpStatus() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CODE_PASSWORD_ERROR))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void validationExceptionShouldReturn400And10001() throws Exception {
        String body = "{\"username\":\"ab\",\"password\":\"123\"}";
        MvcResult result = mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CODE_PARAM_ERROR))
                .andReturn();

        JsonNode node = mapper.readTree(result.getResponse().getContentAsString());
        assertTrue(node.get("message").asText().contains("参数校验失败"));
        assertTrue(node.get("message").asText().contains("username"));
    }

    @Test
    void missingParamShouldReturn400And10002() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CODE_PARAM_MISSING))
                .andExpect(jsonPath("$.message").value("缺少必要参数：name"));
    }

    @Test
    void typeMismatchShouldReturn400And10003() throws Exception {
        mockMvc.perform(get("/test/type?age=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CODE_PARAM_TYPE_ERROR))
                .andExpect(jsonPath("$.message").value("参数类型错误：age"));
    }

    @Test
    void unknownExceptionShouldReturn500And50000() throws Exception {
        mockMvc.perform(get("/test/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(CODE_SYS_ERROR))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后再试"));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        public String business() {
            throw new BusinessException(BizCode.AUTH_PASSWORD_ERROR, "用户名或密码错误", "dev 诊断信息");
        }

        @PostMapping("/validation")
        public String validation(@RequestBody @Valid ValidationDTO dto) {
            return dto.username;
        }

        @GetMapping("/param")
        public String param(@RequestParam String name) {
            return name;
        }

        @GetMapping("/type")
        public String type(@RequestParam Integer age) {
            return String.valueOf(age);
        }

        @GetMapping("/unknown")
        public String unknown() {
            throw new RuntimeException("未知异常");
        }
    }

    static class ValidationDTO {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 64, message = "用户名长度必须在3-64之间")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 32, message = "密码长度必须在8-32之间")
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}