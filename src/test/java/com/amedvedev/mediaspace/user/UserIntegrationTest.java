package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.auth.dto.LoginRequest;
import com.amedvedev.mediaspace.testutil.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.dto.RestoreUserRequest;
import com.amedvedev.mediaspace.user.dto.ChangePasswordRequest;
import com.amedvedev.mediaspace.user.dto.ChangeUsernameRequest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserIntegrationTest extends AbstractIntegrationTest {

    public static final String USERS_ENDPOINT = "/api/users";
    public static final String ME_ENDPOINT = "/me";
    public static final String FOLLOW_ENDPOINT = "/{username}/follow";
    public static final String USERNAME_ENDPOINT = "/username";
    public static final String PASSWORD_ENDPOINT = "/password";
    public static final String RESTORE_ENDPOINT = "/restore";
    @LocalServerPort
    private Integer port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private User user;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = USERS_ENDPOINT;

        clearDbAndRedis();

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
                .get(ME_ENDPOINT)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Full authentication is required to access this resource"));
    }

    @Test
    void shouldNotAccessProtectedEndpointWithInvalidToken() {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + "invalid-token")
                .when()
                .get(ME_ENDPOINT)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Malformed JWT"));
    }

    @Test
    void shouldReturnCurrentUser() {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .get(ME_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("username", equalTo(user.getUsername()));
    }

    @Test
    @Transactional(propagation = Propagation.SUPPORTS)
    void shouldFollowUser() {
        var userToFollow = createUser("user-to-follow");

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .post(FOLLOW_ENDPOINT, userToFollow.getUsername())
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
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .post(FOLLOW_ENDPOINT, userToFollow.getUsername())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .post(FOLLOW_ENDPOINT, userToFollow.getUsername())
                .then()
                .log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("User is already followed"));
    }

    @Test
    void shouldNotFollowNotExistingUser() {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .post(FOLLOW_ENDPOINT, "non-existing-username")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User not found"));
    }

    @Test
    @Transactional(propagation = Propagation.SUPPORTS)
    void shouldUnfollowUser() {
        var userToUnfollow = createUser("user-to-unfollow");

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .post(FOLLOW_ENDPOINT, userToUnfollow.getUsername())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .delete(FOLLOW_ENDPOINT, userToUnfollow.getUsername())
                .then()
                .log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());

        var follower = userRepository.findByUsernameIgnoreCase(user.getUsername()).orElseThrow();
        var followee = userRepository.findByUsernameIgnoreCase(userToUnfollow.getUsername()).orElseThrow();

        Hibernate.initialize(follower.getFollowing());
        Hibernate.initialize(followee.getFollowers());

        assertThat(followee.getFollowers()).doesNotContain(follower);
        assertThat(follower.getFollowing()).doesNotContain(followee);
    }

    @Test
    void shouldNotUnfollowUserIfNotFollowed() {
        var userToUnfollow = createUser("user-to-unfollow");

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .delete(FOLLOW_ENDPOINT, userToUnfollow.getUsername())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("Cannot unfollow user that is not followed"));
    }

    @Test
    void shouldNotUnfollowNotExistingUser() {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .delete(FOLLOW_ENDPOINT, "non-existing-username")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User not found"));
    }

    @Test
    void shouldChangeUserUsernameAlone() {
        var changeUsernameRequest = ChangeUsernameRequest.builder().username("new-username").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(changeUsernameRequest)
                .when()
                .patch(USERNAME_ENDPOINT)
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value());
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForShouldNotChangeUsernameNotMatchingPattern")
    void shouldNotChangeUsernameNotMatchingPattern(ChangeUsernameRequest changeUsernameRequest, String expectedErrorMessage) {
        ChangeUsernameRequest.builder().username("new username").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(changeUsernameRequest)
                .log().all()
                .when()
                .patch(USERNAME_ENDPOINT)
                .then()
                .log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.username", equalTo(expectedErrorMessage));
    }

    static Stream<Arguments> getArgumentsForShouldNotChangeUsernameNotMatchingPattern() {
        return Stream.of(
                Arguments.of(new ChangeUsernameRequest("new username"),
                        "Username must not contain spaces and may only include English letters, digits, underscores (_), hyphens (-), or periods (.)"),

                Arguments.of(new ChangeUsernameRequest("n"),
                        "Username must be between 3 and 20 characters"),

                Arguments.of(new ChangeUsernameRequest("n".repeat(21)),
                        "Username must be between 3 and 20 characters")
        );
    }

    @Test
    void shouldChangeUserPassword() {
        var changePasswordRequest = ChangePasswordRequest.builder()
                .oldPassword("password")
                .password("new-password")
                .build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(changePasswordRequest)
                .when()
                .patch(PASSWORD_ENDPOINT)
                .then()
                .log().all()
                .statusCode(HttpStatus.OK.value());
    }

    @ParameterizedTest
    @MethodSource("getArgumentsForShouldNotChangePasswordNotMatchingPattern")
    void shouldNotChangePasswordNotMatchingPattern(ChangePasswordRequest changeUsernameRequest, String expectedErrorMessage) {
        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(changeUsernameRequest)
                .when()
                .patch(PASSWORD_ENDPOINT)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("errors.password", equalTo(expectedErrorMessage));
    }

    static Stream<Arguments> getArgumentsForShouldNotChangePasswordNotMatchingPattern() {
        return Stream.of(
                Arguments.of(new ChangePasswordRequest("password", "new password"),
                        "Password cannot contain spaces"),

                Arguments.of(new ChangePasswordRequest("password", "n"),
                        "Password must be between 6 and 20 characters"),

                Arguments.of(new ChangePasswordRequest("password", "n".repeat(21)),
                        "Password must be between 6 and 20 characters")
        );
    }

    @Test
    void shouldNotChangeUserWithSameUsername() {
        var changeUserRequest = ChangeUsernameRequest.builder().username(user.getUsername()).build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(changeUserRequest)
                .when()
                .patch(USERNAME_ENDPOINT)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("New username is the same as the old one"));
    }

    @Test
    void shouldNotChangeUserWithTakenUsername() {
        userRepository.save(User.builder().username("taken-username").password(passwordEncoder.encode("password")).build());
        var changeUserRequest = ChangeUsernameRequest.builder().username("taken-username").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(changeUserRequest)
                .when()
                .patch(USERNAME_ENDPOINT)
                .then()
                .log().all()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("Username is already taken"));
    }

    @Test
    void shouldDeleteUser() {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .delete()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void shouldNotAllowDeletedUsersEvenWithToken() {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .delete()
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .get(ME_ENDPOINT)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Your account is deleted. If you want to restore it - use /api/users/restore endpoint."));
    }

    @Test
    void shouldNotAllowLoginWhenUserIsDeleted() {
        var loginRequest = LoginRequest.builder().username("user").password("password").build();

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .delete()
                .then()
                .log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());

        var loginEndpoint = RestAssured.baseURI + ":" + port + "/api/auth/login";
        given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .log().all()
                .when()
                .post(loginEndpoint)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .log().all()
                .body("reason", equalTo("Your account is deleted. If you want to restore it - use /api/users/restore endpoint."));
    }

    @Test
    void shouldRestoreDeletedUserWithoutAuthorization() {
        var restoreUserRequest = RestoreUserRequest.builder().username("user").password("password").build();

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .delete()
                .then()
                .log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .contentType(ContentType.JSON)
                .body(restoreUserRequest)
                .when()
                .put(RESTORE_ENDPOINT)
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
                .put(RESTORE_ENDPOINT)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("Required request body is missing or malformed"));
    }

    @Test
    void shouldSayMethodNotAllowedWhenRestoringUserWithPatch() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .patch(RESTORE_ENDPOINT)
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
                .put(RESTORE_ENDPOINT)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("User is not deleted"));
    }

    @Test
    void shouldNotRestoreUserWithIncorrectPassword() {
        var restoreUserRequest = RestoreUserRequest.builder().username("user").password("wrong-password").build();

        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .delete()
                .then()
                .log().all()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .contentType(ContentType.JSON)
                .body(restoreUserRequest)
                .when()
                .put(RESTORE_ENDPOINT)
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
                .put(RESTORE_ENDPOINT)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User not found"));
    }
}
