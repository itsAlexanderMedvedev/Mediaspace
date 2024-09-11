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
import static org.assertj.core.api.Assertions.assertThat;
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

    public User createUser(String username) {
        return userRepository.save(
                User.builder()
                        .username(username)
                        .password(passwordEncoder.encode("password"))
                        .build()
        );
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
    void shouldFollowUser() {
        var userToFollow = createUser("user-to-follow");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/{username}/follow", userToFollow.getUsername())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        var follower = userRepository.findByUsernameIgnoreCase(user.getUsername()).orElseThrow();
        var followee = userRepository.findByUsernameIgnoreCase(userToFollow.getUsername()).orElseThrow();

        assertThat(followee.getFollowers()).contains(follower);
        assertThat(follower.getFollowing()).contains(followee);
    }

    @Test
    void shouldNotFollowUserIfAlreadyFollowed() {
        var userToFollow = createUser("user-to-follow");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/{username}/follow", userToFollow.getUsername())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/{username}/follow", userToFollow.getUsername())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("User is already followed"));
    }

    @Test
    void shouldNotFollowNotExistingUser() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/{username}/follow", "non-existing-username")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User not found"));
    }

    @Test
    void shouldUnfollowUser() {
        var userToUnfollow = createUser("user-to-unfollow");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post("/{username}/follow", userToUnfollow.getUsername())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/{username}/follow", userToUnfollow.getUsername())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        var follower = userRepository.findByUsernameIgnoreCase(user.getUsername()).orElseThrow();
        var followee = userRepository.findByUsernameIgnoreCase(userToUnfollow.getUsername()).orElseThrow();

        assertThat(followee.getFollowers()).doesNotContain(follower);
        assertThat(follower.getFollowing()).doesNotContain(followee);
    }

    @Test
    void shouldNotUnfollowUserIfNotFollowed() {
        var userToUnfollow = createUser("user-to-unfollow");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/{username}/follow", userToUnfollow.getUsername())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("Cannot unfollow user that is not followed"));
    }

    @Test
    void shouldNotUnfollowNotExistingUser() {
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/{username}/follow", "non-existing-username")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User not found"));
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
    void shouldRestoreDeletedUserWithoutAuthorization() {
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
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User not found"));
    }
}
