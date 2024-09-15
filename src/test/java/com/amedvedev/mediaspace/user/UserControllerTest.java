package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.auth.JwtAuthenticationFilter;
import com.amedvedev.mediaspace.exception.handler.GlobalExceptionHandler;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.dto.UpdateUsernameRequest;
import com.amedvedev.mediaspace.user.dto.UpdateUserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @MockBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updateUserInfo_ShouldReturnNotFound_WhenUserIsNotFound() throws Exception {
        var updateUserDto = new UpdateUsernameRequest("newUsername");

        doThrow(new UserNotFoundException("User not found")).when(userService).updateUsername(any());

        mockMvc.perform(patch("/api/users/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason").value("User not found"));
    }

    @Test
    void updateUserInfo_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        var updateUserDto = UpdateUsernameRequest.builder().username("ab").build();

        mockMvc.perform(patch("/api/users/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserInfo_ShouldReturnOk_WhenUserIsUpdatedSuccessfully() throws Exception {
        var updateUserDto = new UpdateUsernameRequest("newUsername");
        var updateUserResponseDto = new UpdateUserResponse("Username updated successfully, please log in again with new credentials");

        doReturn(updateUserResponseDto).when(userService).updateUsername(any(UpdateUsernameRequest.class));

        mockMvc.perform(patch("/api/users/username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Username updated successfully, please log in again with new credentials"));

        verify(userService).updateUsername(any(UpdateUsernameRequest.class));
    }
}