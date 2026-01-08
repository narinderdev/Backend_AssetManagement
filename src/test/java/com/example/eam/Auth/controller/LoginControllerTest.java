package com.example.eam.Auth.controller;

import com.example.eam.Auth.dto.LoginDto;
import com.example.eam.Auth.dto.LoginResponseDto;
import com.example.eam.Auth.service.LoginService;
import com.example.eam.Auth.service.LogoutService;
import com.example.eam.Exception.GlobalExceptionHandler;
import com.example.eam.User.entity.Users;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LoginService loginService;

    @Mock
    private LogoutService logoutService;

    @InjectMocks
    private LoginController loginController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(loginController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_returnsSuccessBody() throws Exception {
        LoginResponseDto responseDto = new LoginResponseDto("token-123", new Users());
        when(loginService.login(any(LoginDto.class))).thenReturn(responseDto);

        LoginDto dto = new LoginDto();
        dto.setEmail("test@example.com");
        dto.setPassword("secret");

        mockMvc.perform(post("/auth")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.token").value("token-123"));
    }

    @Test
    void logout_withBearerHeader_returnsOk() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Logout Successfully"));

        verify(logoutService).logout("Bearer abc");
    }

    @Test
    void logout_missingHeader_returnsUnauthorized() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization header is missing or invalid"))
                .when(logoutService).logout(null);

        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
