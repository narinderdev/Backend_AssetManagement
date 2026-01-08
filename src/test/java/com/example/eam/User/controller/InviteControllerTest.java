package com.example.eam.User.controller;

import com.example.eam.Exception.GlobalExceptionHandler;
import com.example.eam.User.dto.InviteUserRequest;
import com.example.eam.User.dto.SetPasswordDto;
import com.example.eam.User.entity.UserStatus;
import com.example.eam.User.entity.Users;
import com.example.eam.User.service.InvitationService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InviteControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InvitationService invitationService;

    @InjectMocks
    private InviteController inviteController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(inviteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void inviteUser_returnsSuccessEnvelope() throws Exception {
        Users invited = Users.builder()
                .id(11L)
                .email("guest@example.com")
                .status(UserStatus.INVITED)
                .build();
        when(invitationService.inviteUser(any(InviteUserRequest.class))).thenReturn(invited);

        InviteUserRequest request = new InviteUserRequest();
        request.setFirstName("Guest");
        request.setLastName("User");
        request.setEmail("guest@example.com");
        request.setRoleIds(List.of(1L));

        mockMvc.perform(post("/users/invite")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.email").value("guest@example.com"));
    }

    @Test
    void acceptInvite_redirectsToSetPassword() throws Exception {
        when(invitationService.getSetPasswordRedirectUrl(anyString()))
                .thenReturn("http://frontend/set-password?email=a%40b.com");

        mockMvc.perform(get("/users/accept").param("email", "a@b.com"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://frontend/set-password?email=a%40b.com"));
    }

    @Test
    void setPassword_badInviteReturnsBadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already used"))
                .when(invitationService).setPassword(any(SetPasswordDto.class));

        SetPasswordDto dto = new SetPasswordDto();
        dto.setEmail("guest@example.com");
        dto.setPassword("secret123");

        mockMvc.perform(post("/users/set-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }
}
