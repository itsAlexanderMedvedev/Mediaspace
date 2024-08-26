package com.amedvedev.instagram.user;

import com.amedvedev.instagram.auth.JwtAuthenticationFilter;
import com.amedvedev.instagram.exception.GlobalExceptionHandler;
import com.amedvedev.instagram.exception.UserNotFoundException;
import com.amedvedev.instagram.user.dto.UpdateUserDto;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
        doThrow(new UserNotFoundException("User not found")).when(userService).updateUser(anyString(), any());

        mockMvc.perform(patch("/api/users/nonExistingUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserDto("newUsername", "newEmail@example.com"))))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason").value("User not found"));
    }

    @Test
    void updateUserInfo_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        var updateUserDto = UpdateUserDto.builder().username("ab").build();

        mockMvc.perform(patch("/api/users/existingUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateUserDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserInfo_ShouldReturnOk_WhenUserIsUpdatedSuccessfully() throws Exception {
        mockMvc.perform(patch("/api/users/existingUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserDto("newUsername", "newEmail@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Please, log in again with your new credentials"));

        verify(userService).updateUser(anyString(), any(UpdateUserDto.class));
    }
}