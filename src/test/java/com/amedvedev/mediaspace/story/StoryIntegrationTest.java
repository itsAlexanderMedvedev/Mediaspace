package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.media.MediaRepository;
import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
import com.amedvedev.mediaspace.story.dto.CreateStoryRequest;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import com.amedvedev.mediaspace.testutil.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.User;
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
public class StoryIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private JwtService jwtService;

    private User user;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/stories";

        jdbcTemplate.execute("TRUNCATE story, media, _user RESTART IDENTITY CASCADE");

        user = userRepository.save(User.builder().username("user").password("encoded-password").build());
        token = jwtService.generateToken(user);
    }

    private Story createStory() {
        var media = Media.builder().url("https://example.com").build();

        return storyRepository.save(
                Story.builder()
                        .user(user)
                        .media(media)
                        .build()
        );
    }

    @Test
    void shouldCreateStory() {
        var request = CreateStoryRequest.builder()
                .createMediaRequest(CreateMediaRequest.builder().url("https://example.com").build())
                .build();

        var response = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(ViewStoryResponse.class);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getMediaUrl()).isEqualTo(request.getCreateMediaRequest().getUrl());
    }

    @Test
    void shouldNotCreateStoryWhenUserHasReachedTheLimit() {
        for (int i = 0; i < 30; i++) {
            createStory();
        }

        var request = CreateStoryRequest.builder()
                .createMediaRequest(CreateMediaRequest.builder().url("https://example.com").build())
                .build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(request)
                .post()
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("reason", equalTo("Maximum number of stories reached"));
    }

    @Test
    void getStoryById() {
        var story = createStory();

        var response = given()
                .header("Authorization", "Bearer " + token)
                .get("/" + story.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(ViewStoryResponse.class);

        assertThat(response.getId()).isEqualTo(story.getId());
        assertThat(response.getMediaUrl()).isEqualTo(story.getMedia().getUrl());
    }

    @Test
    void shouldNotGetStoryByIdWhenStoryNotFound() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/{id}", 1)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("Story not found"));
    }

    @Test
    void shouldGetStoriesOfUser() {
        createStory();
        createStory();

        var response = given()
                .header("Authorization", "Bearer " + token)
                .get("/user/" + user.getUsername())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", ViewStoryResponse.class);

        assertThat(response).hasSize(2);
    }

    @Test
    void shouldSayNoStoriesFoundWhenUserHasNoStories() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/user/" + user.getUsername())
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User user has no stories"));
    }

    @Test
    void shouldNotGetStoriesOfUserWhenUserNotFound() {
        given()
                .header("Authorization", "Bearer " + token)
                .get("/user/non-existing-user")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User not found"));
    }

    @Test
    void shouldNotGetStoryByIdWhenUnauthorized() {
        var story = createStory();

        given()
                .get("/{id}", story.getId())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldDeleteStory() {
        var story = createStory();

        given()
                .header("Authorization", "Bearer " + token)
                .delete("/" + story.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        assertThat(storyRepository.findById(story.getId())).isEmpty();
    }

    @Test
    void shouldNotDeleteStoryWhenUnauthorized() {
        var story = createStory();

        given()
                .delete("/{id}", story.getId())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldNotDeleteStoryWhenStoryNotFound() {
        given()
                .header("Authorization", "Bearer " + token)
                .delete("/{id}", 1)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("Story not found"));
    }

    @Test
    void shouldNotDeleteStoryOfOtherUsers() {
        var story = createStory();

        var otherUser = userRepository.save(User.builder().username("another-user").password("encoded-password").build());
        var otherUserToken = jwtService.generateToken(otherUser);

        given()
                .header("Authorization", "Bearer " + otherUserToken)
                .delete("/{id}", story.getId())
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("reason", equalTo("Cannot delete story of another user"));
    }
}