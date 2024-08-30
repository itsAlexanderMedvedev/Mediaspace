package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.testutils.AbstractIntegrationTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserIntegrationTests extends AbstractIntegrationTest {

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
    void shouldNotAccessProtectedEndpointWithoutToken() {
        given()
                .when()
                .get("/secured-endpoint")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Full authentication is required to access this resource"));
    }

    @Test
    void shouldNotAccessProtectedEndpointWithInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid-token")
                .when()
                .get("/secured-endpoint")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Malformed JWT"));
    }
}
