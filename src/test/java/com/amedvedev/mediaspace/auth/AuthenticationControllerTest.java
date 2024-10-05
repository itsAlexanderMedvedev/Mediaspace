package com.amedvedev.mediaspace.auth;

import com.amedvedev.mediaspace.auth.dto.LoginRequest;
import com.amedvedev.mediaspace.auth.dto.LoginResponse;
import com.amedvedev.mediaspace.auth.dto.RegisterRequest;
import com.amedvedev.mediaspace.auth.dto.RegisterResponse;
import com.amedvedev.mediaspace.exception.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthenticationController.class)
class AuthenticationControllerTest {

    public static final String REGISTER_ENDPOINT = "/api/auth/register";
    public static final String LOGIN_ENDPOINT = "/api/auth/login";

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mocked to prevent actual behavior
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
    void registerShouldReturnBadRequestWhenUsernameOfInvalidLengthOrEmpty(RegisterRequest request) throws Exception {
        mockMvc.perform(post(REGISTER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username")
                        .value("Username must be between 3 and 20 characters"));

        verify(authenticationService, Mockito.never()).register(any(RegisterRequest.class));
    }

    static RegisterRequest[] getArgumentsForRegisterTestInvalidUsernameLength() {
        return new RegisterRequest[] {
                new RegisterRequest("a", "password"),
                new RegisterRequest("a".repeat(21), "password"),
                new RegisterRequest("", "password")
        };
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRegisterTestInvalidUsernameCharacters")
    void registerShouldReturnBadRequestWhenUsernameHasInvalidCharacters(RegisterRequest request) throws Exception {

        mockMvc.perform(post(REGISTER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Username must not contain spaces and may only include English letters, digits, underscores (_), hyphens (-), or periods (.)"));

        verify(authenticationService, Mockito.never()).register(any(RegisterRequest.class));
    }

    static RegisterRequest[] getArgumentsForRegisterTestInvalidUsernameCharacters() {
        return new RegisterRequest[]{
                new RegisterRequest("abc abc", "password"),
                new RegisterRequest("\n\t ", "password"),
                new RegisterRequest("abc$", "password"),
                new RegisterRequest("абв", "password")
        };
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRegisterTestPasswordInvalidLength")
    void registerShouldReturnBadRequestWhenPasswordInvalidLength(RegisterRequest request) throws Exception {

        mockMvc.perform(post(REGISTER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password must be between 6 and 20 characters"));

        verify(authenticationService, Mockito.never()).register(any(RegisterRequest.class));
    }

    static RegisterRequest[] getArgumentsForRegisterTestPasswordInvalidLength() {
        return new RegisterRequest[]{
                new RegisterRequest("username", "1".repeat(5)),
                new RegisterRequest("username", "1".repeat(21))
        };
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForRegisterTestPasswordContainsSpaces")
    void registerShouldReturnBadRequestWhenPasswordContainsSpaces(RegisterRequest request) throws Exception {

        mockMvc.perform(post(REGISTER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").value("Password cannot contain spaces"));

        verify(authenticationService, Mockito.never()).register(any(RegisterRequest.class));
    }

    static RegisterRequest[] getArgumentsForRegisterTestPasswordContainsSpaces() {
        return new RegisterRequest[]{
                new RegisterRequest("username", "password "),
                new RegisterRequest("username", " password"),
                new RegisterRequest("username", " ".repeat(6)),
                new RegisterRequest("username", "pass\tp")
        };
    }

    @Test
    void registerShouldReturnCreatedStatusAndMessageWhenInputIsValid() throws Exception {
        var request = new RegisterRequest("username", "password");
        var response = new RegisterResponse("User registered successfully");

        Mockito.when(authenticationService.register(any())).thenReturn(response);

        mockMvc.perform(post(REGISTER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void loginShouldReturnOkStatusAndTokenWhenInputIsValid() throws Exception {
        var request = new LoginRequest("username", "password");
        var response = new LoginResponse("mock-token");

        Mockito.when(authenticationService.login(any())).thenReturn(response);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-token"));
    }

    @Test
    void loginShouldReturnUnauthorizedStatusWhenCredentialsAreInvalid() throws Exception {
        var request = new LoginRequest("username", "password");

        Mockito.when(authenticationService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.reason").value("Bad credentials"));
    }
}