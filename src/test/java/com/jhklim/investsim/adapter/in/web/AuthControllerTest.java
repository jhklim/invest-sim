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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    @DisplayName("유효한 refreshToken으로 새 accessToken과 새 refreshToken을 발급받는다")
    void refresh_withValidToken_returnsNewTokens() throws Exception {
        JsonNode tokens = signupAndLogin("refresh_test@gmail.com", "password123");
        Long memberId = memberRepository.findByEmail("refresh_test@gmail.com").get().getId();
        String refreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "memberId", memberId,
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("재발급 후 기존 refreshToken으로 재시도 시 401을 반환한다")
    void refresh_withOldToken_returns401() throws Exception {
        JsonNode tokens = signupAndLogin("rotation_test@gmail.com", "password123");
        Long memberId = memberRepository.findByEmail("rotation_test@gmail.com").get().getId();
        String oldRefreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "memberId", memberId,
                                "refreshToken", oldRefreshToken
                        ))))
                .andExpect(status().isOk());

        // 기존 RT로 재시도 → 이미 새 RT로 교체됐으므로 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "memberId", memberId,
                                "refreshToken", oldRefreshToken
                        ))))
                .andExpect(status().isUnauthorized());
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
        String accessToken = tokens.get("accessToken").asText();
        String refreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "memberId", memberId,
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 후 블랙리스트된 accessToken으로 API 호출 시 401을 반환한다")
    void logout_thenAccessTokenBlacklisted_returns401() throws Exception {
        JsonNode tokens = signupAndLogin("blacklist_test@gmail.com", "password123");
        String accessToken = tokens.get("accessToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/trades")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }
}