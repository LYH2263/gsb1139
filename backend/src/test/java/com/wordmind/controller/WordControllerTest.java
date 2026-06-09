package com.wordmind.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordmind.dto.AuthDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // 获取用户 Token
        AuthDTO.LoginRequest userLogin = new AuthDTO.LoginRequest();
        userLogin.setUsername("user");
        userLogin.setPassword("user123");
        MvcResult userResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
                .andReturn();
        userToken = objectMapper.readTree(userResult.getResponse().getContentAsString())
                .get("data").get("token").asText();

        // 获取管理员 Token
        AuthDTO.LoginRequest adminLogin = new AuthDTO.LoginRequest();
        adminLogin.setUsername("admin");
        adminLogin.setPassword("admin123");
        MvcResult adminResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin)))
                .andReturn();
        adminToken = objectMapper.readTree(adminResult.getResponse().getContentAsString())
                .get("data").get("token").asText();
    }

    @Test
    void testGetWordsWithAuth() throws Exception {
        mockMvc.perform(get("/api/words")
                .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testUserCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(post("/api/admin/words")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word\":\"test\",\"meaning\":\"测试\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/words")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateDuplicateWordShouldFail() throws Exception {
        // 使用一个本次测试唯一的单词，先成功创建一次
        String uniqueWord = "duplicateTestWord_" + System.nanoTime();
        String body = "{\"word\":\"" + uniqueWord + "\",\"meaning\":\"重复测试\"}";

        mockMvc.perform(post("/api/admin/words")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 第二次创建相同的单词应当返回明确的"单词已存在"错误
        mockMvc.perform(post("/api/admin/words")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("单词已存在"));
    }

    @Test
    void testCreateExistingSeedWordShouldFail() throws Exception {
        // DataInitializer 中已经初始化了 "happy" 单词，再次创建应当失败
        mockMvc.perform(post("/api/admin/words")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"word\":\"happy\",\"meaning\":\"快乐的\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("单词已存在"));
    }
}
