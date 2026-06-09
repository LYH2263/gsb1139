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
        AuthDTO.LoginRequest userLogin = new AuthDTO.LoginRequest();
        userLogin.setUsername("user");
        userLogin.setPassword("user123");
        MvcResult userResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLogin)))
                .andReturn();
        userToken = objectMapper.readTree(userResult.getResponse().getContentAsString())
                .get("data").get("token").asText();

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
        String uniqueWord = "testword_" + System.currentTimeMillis();
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("单词已存在"));
    }

    @Test
    void testUpdateWordToDuplicateShouldFail() throws Exception {
        String word1 = "word1_" + System.currentTimeMillis();
        String word2 = "word2_" + System.currentTimeMillis();
        
        WordDTO.CreateRequest createReq1 = WordDTO.CreateRequest.builder()
                .word(word1)
                .meaning("释义1")
                .build();
        WordDTO.CreateRequest createReq2 = WordDTO.CreateRequest.builder()
                .word(word2)
                .meaning("释义2")
                .build();

        MvcResult result1 = mockMvc.perform(post("/api/admin/words")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq1)))
                .andExpect(status().isOk())
                .andReturn();
        Long id1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("data").get("id").asLong();

        mockMvc.perform(post("/api/admin/words")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq2)))
                .andExpect(status().isOk());

        WordDTO.UpdateRequest updateReq = WordDTO.UpdateRequest.builder()
                .word(word2)
                .build();

        mockMvc.perform(put("/api/admin/words/" + id1)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("单词已存在"));
    }
}
