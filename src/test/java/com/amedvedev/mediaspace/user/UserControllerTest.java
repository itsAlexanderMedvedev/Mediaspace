package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.auth.JwtAuthenticationFilter;
import com.amedvedev.mediaspace.exception.handler.GlobalExceptionHandler;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.dto.ChangeUsernameRequest;
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

    public static final String USER_ENDPOINT = "/api/users/username";
    @MockBean
    private UserService userService;

    @MockBean
    private UserProfileService userProfileService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mocked to prevent actual behavior
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, userProfileService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void changeUsernameInfoShouldReturnNotFoundWhenUserIsNotFound() throws Exception {
        var updateUserDto = new ChangeUsernameRequest("newUsername");
        doThrow(new UserNotFoundException("User not found")).when(userService).changeUsername(any());

        mockMvc.perform(patch(USER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason").value("User not found"));
    }

    @Test
    void changeUsernameShouldReturnBadRequestWhenValidationFails() throws Exception {
        var updateUserDto = ChangeUsernameRequest.builder().username("ab").build();

        mockMvc.perform(patch(USER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeUsernameShouldReturnOkWhenUsernameIsChangedSuccessfully() throws Exception {
        var changeUsernameRequest = new ChangeUsernameRequest("newUsername");
        var updateUserResponseDto = new UpdateUserResponse("Username changed successfully, please log in again with new credentials");
        doReturn(updateUserResponseDto).when(userService).changeUsername(any(ChangeUsernameRequest.class));

        mockMvc.perform(patch(USER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeUsernameRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Username changed successfully, please log in again with new credentials"));

        verify(userService).changeUsername(any(ChangeUsernameRequest.class));
    }
}