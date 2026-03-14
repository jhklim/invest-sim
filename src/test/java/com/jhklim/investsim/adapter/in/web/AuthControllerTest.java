package com.jhklim.investsim.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jhklim.investsim.adapter.out.persistence.jpa.MemberRepository;
import com.jhklim.investsim.adapter.out.redis.RefreshTokenStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    private JsonNode signupAndLogin(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password,
                                "nickname", "tester"
                        ))))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("로그인 시 accessToken과 refreshToken이 반환된다")
    void login_returnsAccessTokenAndRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "auth_test@gmail.com",
                                "password", "password123",
                                "nickname", "tester"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "auth_test@gmail.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("유효한 refreshToken으로 새 accessToken을 발급받는다")
    void refresh_withValidToken_returnsNewAccessToken() throws Exception {
        JsonNode tokens = signupAndLogin("refresh_test@gmail.com", "password123");
        Long memberId = memberRepository.findByEmail("refresh_test@gmail.com").get().getId();
        String refreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "memberId", memberId,
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("잘못된 refreshToken으로 재발급 요청 시 401을 반환한다")
    void refresh_withInvalidToken_returns401() throws Exception {
        JsonNode tokens = signupAndLogin("refresh_invalid@gmail.com", "password123");
        Long memberId = memberRepository.findByEmail("refresh_invalid@gmail.com").get().getId();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "memberId", memberId,
                                "refreshToken", "wrong-token"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 후 refreshToken으로 재발급 요청 시 401을 반환한다")
    void logout_thenRefresh_returns401() throws Exception {
        JsonNode tokens = signupAndLogin("logout_test@gmail.com", "password123");
        Long memberId = memberRepository.findByEmail("logout_test@gmail.com").get().getId();
        String refreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "memberId", memberId
                        ))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "memberId", memberId,
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isUnauthorized());
    }
}