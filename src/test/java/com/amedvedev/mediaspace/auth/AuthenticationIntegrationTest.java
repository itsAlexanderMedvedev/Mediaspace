package com.amedvedev.mediaspace.auth;

import com.amedvedev.mediaspace.auth.dto.LoginRequest;
import com.amedvedev.mediaspace.auth.dto.LoginResponse;
import com.amedvedev.mediaspace.auth.dto.RegisterRequest;
import com.amedvedev.mediaspace.testutils.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    UserRepository userRepository;

    private String securedPath;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/auth";
        securedPath = RestAssured.baseURI + ":" + port + "/api/users/me";

        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUser() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest("username", "password"))
                .when()
                .post("/register")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("message", equalTo("User registered successfully"));

        var user = userRepository.findByUsernameIgnoreCase("username").orElseThrow();
        assertThat(userRepository.findById(user.getId())).isPresent();
        assertThat(userRepository.findById(user.getId()).orElseThrow()).isEqualTo(user);
    }

    @Test
    void shouldLoginUser() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest("test", "password"))
                .when()
                .post("/register")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("message", equalTo("User registered successfully"));

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("test", "password"))
                .when()
                .post("/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(LoginResponse.class);
    }

    @Test
    void shouldNotRegisterUserWithExistingUsername() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest("username", "password"))
                .when()
                .post("/register")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("message", equalTo("User registered successfully"));

        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest("username", "password"))
                .when()
                .post("/register")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("reason", equalTo("This username is already taken"));
    }

    @Test
    void shouldNotLoginWithIncorrectPassword() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest("username", "password"))
                .when()
                .post("/register")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("message", equalTo("User registered successfully"));

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("username", "incorrect"))
                .when()
                .post("/login")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Bad credentials"));
    }

    @Test
    void shouldNotLoginWithIncorrectUsername() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest("username", "password"))
                .when()
                .post("/register")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("message", equalTo("User registered successfully"));

        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("incorrect", "password"))
                .when()
                .post("/login")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Bad credentials"));
    }

    @Test
    void shouldNotLoginWithNonExistingUser() {
        given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("username", "password"))
                .when()
                .post("/login")
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
                .header("Authorization", "Bearer invalid")
                .when()
                .get(securedPath)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Malformed JWT"));
    }

    @Test
    void shouldReturnMe() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest("username", "password"))
                .when()
                .post("/register")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("message", equalTo("User registered successfully"));

        var authenticationResponse = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("username", "password"))
                .when()
                .post("/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(LoginResponse.class);

        given()
                .header("Authorization", "Bearer " + authenticationResponse.getToken())
                .when()
                .get(securedPath)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("username", equalTo("username"));
    }
}
