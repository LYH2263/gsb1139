package com.wordmind.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordmind.dto.AuthDTO;
import com.wordmind.dto.WordDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

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
    void testCreateDuplicateWordReturnsConflict() throws Exception {
        String uniqueWord = "dup_test_" + UUID.randomUUID().toString().substring(0, 8);

        WordDTO.CreateRequest request = WordDTO.CreateRequest.builder()
                .word(uniqueWord)
                .meaning("测试释义")
                .build();

        mockMvc.perform(post("/api/admin/words")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(post("/api/admin/words")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("单词已存在: " + uniqueWord));
    }
}
