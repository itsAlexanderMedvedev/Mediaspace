package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.testutil.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.dto.UpdateUserRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private User user;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/users";

        userRepository.deleteAll();

        user = userRepository.save(User.builder().username("user").password("encoded-password").build());
        token = jwtService.generateToken(user);
    }

    @Test
    void shouldNotAccessProtectedEndpointWithoutToken() {
        given()
                .when()
                .get("/me")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Full authentication is required to access this resource"));
    }

    @Test
    void shouldNotAccessProtectedEndpointWithInvalidToken() {
        given()
                .header("Authorization", "Bearer invalid-token")
                .when()
                .get("/me")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Malformed JWT"));
    }

    @Test
    void shouldReturnCurrentUser() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/me")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("username", equalTo(user.getUsername()));
    }

    @Test
    void shouldUpdateUserUsernameAlone() {
        var updateUserRequest = UpdateUserRequest.builder().username("new-username").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updateUserRequest)
                .when()
                .patch("/{username}", user.getUsername())
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("username", equalTo("new-username"));
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForShouldNotUpdateUsernameNotMatchingPattern")
    void shouldNotUpdateUsernameNotMatchingPattern(UpdateUserRequest updateUserRequest, String expectedErrorMessage) {
        UpdateUserRequest.builder().username("new username").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updateUserRequest)
                .log().all()
                .when()
                .patch("/{username}", user.getUsername())
                .then()
                .log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.username", equalTo(expectedErrorMessage));
    }

    static Stream<Arguments> getArgumentsForShouldNotUpdateUsernameNotMatchingPattern() {
        return Stream.of(
                Arguments.of(new UpdateUserRequest("new username", null),
                        "Username must not contain spaces and may only include English letters, digits, underscores (_), hyphens (-), or periods (.)"),

                Arguments.of(new UpdateUserRequest("n", null),
                        "Username must be between 3 and 20 characters"),

                Arguments.of(new UpdateUserRequest("n".repeat(21), null),
                        "Username must be between 3 and 20 characters")
        );
    }

    @Test
    void shouldUpdateUserPasswordAlone() {
        var updateUserRequest = UpdateUserRequest.builder().password("new-password").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updateUserRequest)
                .when()
                .patch("/{username}", user.getUsername())
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .body("username", equalTo(user.getUsername()));
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForShouldNotUpdatePasswordNotMatchingPattern")
    void shouldNotUpdatePasswordNotMatchingPattern(UpdateUserRequest updateUserRequest, String expectedErrorMessage) {
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updateUserRequest)
                .log().all()
                .when()
                .log().all()
                .patch("/{username}", user.getUsername())
                .then()
                .log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.password", equalTo(expectedErrorMessage));
    }

    static Stream<Arguments> getArgumentsForShouldNotUpdatePasswordNotMatchingPattern() {
        return Stream.of(
                Arguments.of(new UpdateUserRequest(null, "new password"),
                        "Password cannot contain spaces"),

                Arguments.of(new UpdateUserRequest(null, "n"),
                        "Password must be between 6 and 20 characters"),

                Arguments.of(new UpdateUserRequest(null, "n".repeat(21)),
                        "Password must be between 6 and 20 characters")
        );
    }

    @Test
    void shouldNotUpdateUserWithSameUsername() {
        var updateUserRequest = UpdateUserRequest.builder().username(user.getUsername()).build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updateUserRequest)
                .when()
                .patch("/{username}", user.getUsername())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("New username is the same as the old one"));
    }

    @Test
    void shouldNotUpdateUserWithTakenUsername() {
        userRepository.save(User.builder().username("taken-username").password("encoded-password").build());
        var updateUserRequest = UpdateUserRequest.builder().username("taken-username").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updateUserRequest)
                .when()
                .patch("/{username}", user.getUsername())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("Username is already taken"));
    }

    @Test
    void shouldDeleteUser() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }
}
