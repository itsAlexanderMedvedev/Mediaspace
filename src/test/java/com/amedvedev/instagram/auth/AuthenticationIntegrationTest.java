package com.amedvedev.instagram.auth;

import com.amedvedev.instagram.testutils.AbstractIntegrationTest;
import com.amedvedev.instagram.user.UserRepository;
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

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/auth";
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
    void shouldLoginUserAndPerformRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(new RegisterRequest("test", "password"))
                .when()
                .post("/register")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("message", equalTo("User registered successfully"));

        var authenticationResponse = given()
                .contentType(ContentType.JSON)
                .body(new LoginRequest("test", "password"))
                .when()
                .post("/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(AuthenticationResponse.class);

        given()
                .header("Authorization", "Bearer " + authenticationResponse.getToken())
                .when()
                .get("/me")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("username", equalTo("test"));

    }

}
