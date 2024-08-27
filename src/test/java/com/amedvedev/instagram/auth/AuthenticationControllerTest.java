package com.amedvedev.instagram.auth;

import com.amedvedev.instagram.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthenticationController.class)
class AuthenticationControllerTest {

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthenticationController(authenticationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRegisterTestInvalidUsernameLength")
    void registerShouldReturnBadRequestWhenInvalidLengthOrEmpty(RegisterRequest request) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Username must be between 3 and 20 characters"));

        verify(authenticationService, Mockito.never()).register(any(RegisterRequest.class));
    }

    static Stream<Arguments> getArgumentsForRegisterTestInvalidUsernameLength() {
        return Stream.of(
                Arguments.of(new RegisterRequest("a", "password")),
                Arguments.of(new RegisterRequest("a".repeat(21), "password")),
                Arguments.of(new RegisterRequest("", "password"))
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRegisterTestInvalidUsernameCharacters")
    void registerShouldReturnBadRequestWhenUsernameHasInvalidCharacters(RegisterRequest request) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Username must not contain spaces and may only include English letters, digits, underscores (_), hyphens (-), or periods (.)"));

        verify(authenticationService, Mockito.never()).register(any(RegisterRequest.class));
    }

    static Stream<Arguments> getArgumentsForRegisterTestInvalidUsernameCharacters() {
        return Stream.of(
                Arguments.of(new RegisterRequest("abc abc", "password")),
                Arguments.of(new RegisterRequest("\n\t ", "password")),
                Arguments.of(new RegisterRequest("abc$", "password")),
                Arguments.of(new RegisterRequest("абв", "password"))
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRegisterTestPasswordInvalidLength")
    void registerShouldReturnBadRequestWhenPasswordInvalidLength(RegisterRequest request) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password must be between 6 and 20 characters"));

        verify(authenticationService, Mockito.never()).register(any(RegisterRequest.class));
    }

    static Stream<Arguments> getArgumentsForRegisterTestPasswordInvalidLength() {
        return Stream.of(
                Arguments.of(new RegisterRequest("username", "1".repeat(5))),
                Arguments.of(new RegisterRequest("username", "1".repeat(21)))
        );
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRegisterTestPasswordContainsSpaces")
    void registerShouldReturnBadRequestWhenPasswordContainsSpaces(RegisterRequest request) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password cannot contain spaces"));

        verify(authenticationService, Mockito.never()).register(any(RegisterRequest.class));
    }

    static Stream<Arguments> getArgumentsForRegisterTestPasswordContainsSpaces() {
        return Stream.of(
                Arguments.of(new RegisterRequest("username", "password ")),
                Arguments.of(new RegisterRequest("username", " password")),
                Arguments.of(new RegisterRequest("username", "          ")),
                Arguments.of(new RegisterRequest("username", "pass\tp"))
        );
    }

    @Test
    void registerShouldReturnCreatedStatusAndMessageWhenInputIsValid() throws Exception {
        var request = new RegisterRequest("username", "password");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void loginShouldReturnOkStatusAndTokenWhenInputIsValid() throws Exception {
        var request = new LoginRequest("username", "password");

        var response = new AuthenticationResponse("mock-token");

        Mockito.when(authenticationService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-token"));
    }

    @Test
    void loginShouldReturnUnauthorizedStatusWhenCredentialsAreInvalid() throws Exception {
        var request = new LoginRequest("username", "password");

        Mockito.when(authenticationService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("Bad credentials"));
    }
}