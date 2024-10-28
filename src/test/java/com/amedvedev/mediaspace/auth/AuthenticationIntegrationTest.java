package com.amedvedev.mediaspace.auth;

import com.amedvedev.mediaspace.auth.dto.LoginRequest;
import com.amedvedev.mediaspace.auth.dto.LoginResponse;
import com.amedvedev.mediaspace.auth.dto.RegisterRequest;
import com.amedvedev.mediaspace.testutil.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    public static final String ME_ENDPOINT = "/api/users/me";
    public static final String USERS_ENDPOINT = "/api/users";
    public static final String REGISTER_ENDPOINT = "/register";
    public static final String AUTH_ENDPOINT = "/api/auth";
    public static final String LOGIN_ENDPOINT = "/login";

    @LocalServerPort
    private Integer port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String securedPath;

    private String deletePath;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = AUTH_ENDPOINT;
        
        securedPath = RestAssured.baseURI + ":" + port + ME_ENDPOINT;
        deletePath = RestAssured.baseURI + ":" + port + USERS_ENDPOINT;

        clearDbAndRedis();

        System.out.println(redisTemplate.keys("*"));
    }

    private void registerUser(String username, String password) {
        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest(username, password))
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .log().all()
                .statusCode(HttpStatus.CREATED.value())
                .body("message", equalTo("User registered successfully"));
    }

    private LoginResponse loginUser(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest(username, password))
                .when()
                .post(LOGIN_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(LoginResponse.class);
    }

    private void deleteUser(String token) {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .delete(deletePath)
                .then()
                .log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void shouldRegisterUser() {
        registerUser("username", "password");

        var user = userRepository.findByUsernameIgnoreCase("username").orElseThrow();
        assertThat(userRepository.findById(user.getId())).isPresent();
        assertThat(userRepository.findById(user.getId()).orElseThrow()).isEqualTo(user);
    }

    @Test
    void shouldLoginUser() {
        registerUser("test", "password");

        loginUser("test", "password");
    }

    @Test
    void shouldNotRegisterUserWithExistingUsername() {
        registerUser("username", "password");

        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest("username", "password"))
                .when()
                .post(REGISTER_ENDPOINT)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("reason", equalTo("This username is already taken"));
    }

    @Test
    void shouldNotLoginWithIncorrectPassword() {
        registerUser("username", "password");

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("username", "incorrect"))
                .when()
                .post(LOGIN_ENDPOINT)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Bad credentials"));
    }

    @Test
    void shouldNotLoginWithIncorrectUsername() {
        registerUser("username", "password");

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("incorrect", "password"))
                .when()
                .post(LOGIN_ENDPOINT)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Bad credentials"));
    }

    @Test
    void shouldNotLoginNonExistingUser() {
        
        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("username", "password"))
                .when()
                .post(LOGIN_ENDPOINT)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Bad credentials"));
    }

    @Test
    void shouldNotPerformRequestWithoutAuthorization() {
        given()
                .when()
                .get(securedPath)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Full authentication is required to access this resource"));
    }
    
    @Test
    void shouldNotPerformRequestWithInvalidToken() {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + "invalid")
                .when()
                .get(securedPath)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Malformed JWT"));
    }

    @Test
    void shouldReturnMe() {
        registerUser("username", "password");
        var token = loginUser("username", "password").getToken();

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .get(securedPath)
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .body("username", equalTo("username"));
    }

    @Test
    void shouldNotAllowDeletedUsersAccessEndpoints() {
        registerUser("username", "password");
        var token = loginUser("username", "password").getToken();
        deleteUser(token);

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .get(securedPath)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Your account is deleted. If you want to restore it - use /api/users/restore endpoint."));
    }
}
