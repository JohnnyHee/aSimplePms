package com.jyh.pms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jyh.pms.web.dto.req.LoginRequest;
import com.jyh.pms.web.dto.req.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端冒烟测试：dev 环境（内存存储 + 内存令牌）下的登录、鉴权、人员接口与审计联动。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PmsApplicationSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 未携带令牌访问受保护接口返回统一401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2000));
    }

    @Test
    void 登录失败返回业务错误码2001() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(2001));
    }
    @Test
    void 管理员登录后可访问人员列表并看到初始化账号() throws Exception {
        String token = loginAsAdmin();

        MvcResult result = mockMvc.perform(get("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andReturn();

        // 不锁定下标：同一次 JVM 内其它用例可能已新增账号（内存存储为进程级共享）
        List<String> usernames = JsonPath.read(result.getResponse().getContentAsString(),
                "$.data.records[*].username");
        assertThat(usernames).contains("admin");
        assertThat((Integer) JsonPath.read(result.getResponse().getContentAsString(), "$.data.total"))
                .isEqualTo(usernames.size());
    }

    @Test
    void 新增人员后可用新账号登录并返回角色权限() throws Exception {
        String token = loginAsAdmin();

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("tester01");
        request.setPassword("Tester@123");
        request.setName("测试员工");
        request.setDepartment("研发部");

        mockMvc.perform(post("/api/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("tester01"))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("tester01", "Tester@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username").value("tester01"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));

        // 普通员工没有 user:view 权限，应被拒绝
        String employeeToken = login("tester01", "Tester@123");
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(3000));
    }

    @Test
    void 登录后写入审计日志并可查询() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(get("/api/audit-logs").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    private String loginAsAdmin() throws Exception {
        return login("admin", "Admin@123");
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = node.path("data").path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
