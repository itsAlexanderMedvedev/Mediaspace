package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.auth.dto.LoginRequest;
import com.amedvedev.mediaspace.testutil.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.dto.RestoreUserRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/users";

        jdbcTemplate.execute("TRUNCATE post, media, _user RESTART IDENTITY CASCADE");

        user = userRepository.save(User.builder().username("user").password(passwordEncoder.encode("password")).build());
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
                .patch()
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
                .patch()
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
                .patch()
                .then()
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
                .when()
                .patch()
                .then()
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
                .patch()
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("New username is the same as the old one"));
    }

    @Test
    void shouldNotUpdateUserWithTakenUsername() {
        userRepository.save(User.builder().username("taken-username").password(passwordEncoder.encode("password")).build());
        var updateUserRequest = UpdateUserRequest.builder().username("taken-username").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updateUserRequest)
                .when()
                .patch()
                .then()
                .log().all()
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

    @Test
    void shouldNotAllowDeletedUsersEvenWithToken() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/me")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Your account is deleted. If you want to restore it - use /api/users/restore endpoint."));
    }

    @Test
    void shouldNotAllowLoginWhenUserIsDeleted() {
        var loginRequest = LoginRequest.builder().username("user").password("password").build();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post(RestAssured.baseURI + ":" + port + "/api/auth/login")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Your account is deleted. If you want to restore it - use /api/users/restore endpoint."));
    }

    @Test
    void shouldRestoreDeletedUser() {
        var restoreUserRequest = RestoreUserRequest.builder().username("user").password("password").build();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .contentType(ContentType.JSON)
                .body(restoreUserRequest)
                .when()
                .put("/restore")
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value())
                .body("message", equalTo("User restored successfully. Please login to continue."));
    }

    @Test
    void shouldSayRequestBodyMissingWhenRestoringUserWithoutBody() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .put("/restore")
                .then()
                .log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("Required request body is missing or malformed"));
    }

    @Test
    void shouldSayMethodNotAllowedWhenRestoringUserWithPatch() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch("/restore")
                .then()
                .log().all()
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
                .body("reason", equalTo("Request method 'PATCH' is not supported"));
    }

    @Test
    void shouldNotRestoreNonDeletedUser() {
        var restoreUserRequest = RestoreUserRequest.builder().username("user").password("password").build();

        given()
                .contentType(ContentType.JSON)
                .body(restoreUserRequest)
                .when()
                .put("/restore")
                .then()
                .log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("User is not deleted"));
    }

    @Test
    void shouldNotRestoreUserWithIncorrectPassword() {
        var restoreUserRequest = RestoreUserRequest.builder().username("user").password("wrong-password").build();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .contentType(ContentType.JSON)
                .body(restoreUserRequest)
                .when()
                .put("/restore")
                .then()
                .log().all()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Bad credentials"));
    }

    @Test
    void shouldNotRestoreUserWithNonExistingUsername() {
        var restoreUserRequest = RestoreUserRequest.builder().username("non-existing-username").password("password").build();

        given()
                .contentType(ContentType.JSON)
                .body(restoreUserRequest)
                .when()
                .put("/restore")
                .then()
                .log().all()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User not found"));
    }
}
