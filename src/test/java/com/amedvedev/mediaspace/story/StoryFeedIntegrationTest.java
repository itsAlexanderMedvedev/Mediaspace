package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
import com.amedvedev.mediaspace.story.dto.CreateStoryRequest;
import com.amedvedev.mediaspace.story.dto.StoriesFeedEntry;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.service.StoryRedisService;
import com.amedvedev.mediaspace.testutil.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserRepository;
import com.amedvedev.mediaspace.user.follow.FollowRepository;
import com.amedvedev.mediaspace.user.service.UserService;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Transactional
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StoryFeedIntegrationTest extends AbstractIntegrationTest {

    private static final String STORIES_FEED_ENDPOINT = "/feed/stories";
    private static final String FOLLOW_ENDPOINT = "/users/{username}/follow";
    public static final String STORIES_ENDPOINT = "/stories";

    @LocalServerPort
    private Integer port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private StoryRedisService storyRedisService;

    @Autowired
    private StoryMapper storyMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private JwtService jwtService;

    private User user1;
    private User user2;
    private User user3;
    private User user4;

    private String token1;
    private String token2;
    private String token3;
    private String token4;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api";

        clearDbAndRedis();

        executeInsideTransaction(() -> {
            user1 = userRepository.save(User.builder().username("user1").password("encoded-password").build());
            user2 = userRepository.save(User.builder().username("user2").password("encoded-password").build());
            user3 = userRepository.save(User.builder().username("user3").password("encoded-password").build());
            user4 = userRepository.save(User.builder().username("user4").password("encoded-password").build());
            return null;
        });

        token1 = jwtService.generateToken(user1);
        token2 = jwtService.generateToken(user2);
        token3 = jwtService.generateToken(user3);
        token4 = jwtService.generateToken(user4);
    }

    private void createStoryForUser(String token) {
        var createStoryRequest = CreateStoryRequest.builder()
                .createMediaRequest(CreateMediaRequest.builder().url("https://example.com").build())
                .build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(createStoryRequest)
                .when()
                .post(STORIES_ENDPOINT)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract()
                .as(StoryDto.class);
    }

    private void followUserWithRequest(String token, User followee) {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .post(FOLLOW_ENDPOINT, followee.getUsername())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    private void waitForAsyncStoryCache() {
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> storyRedisService.getStoriesFeedByUserId(user1.getId())
                        .map(feed -> feed.size() == 2)
                        .orElse(false));
    }

    @Test
    void getStoriesFeed() {
        followUserWithRequest(token1, user2);
        followUserWithRequest(token1, user3);
        followUserWithRequest(token1, user4); // user4 is not posting anything
        followUserWithRequest(token3, user2); // user3 is following user2, should not affect user1's feed

        createStoryForUser(token3);
        createStoryForUser(token3);
        createStoryForUser(token2);

        var storiesFeedEntryUser2 = StoriesFeedEntry.builder()
                .username(user2.getUsername())
                .profilePictureUrl(user2.getProfilePictureUrl())
                .build();
        var storiesFeedEntryUser3 = StoriesFeedEntry.builder()
                .username(user3.getUsername())
                .profilePictureUrl(user3.getProfilePictureUrl())
                .build();
        
        waitForAsyncStoryCache();

        
        var feed1 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoriesFeedEntry.class);


        assertThat(feed1).hasSize(2);
        assertThat(feed1.get(0)).isEqualTo(storiesFeedEntryUser2); // latest story goes first
        assertThat(feed1.get(1)).isEqualTo(storiesFeedEntryUser3);
    }

    @Test
    void getStoriesFeedIsEmptyWhenNoStoriesPosted() {
        followUserWithRequest(token1, user2);
        followUserWithRequest(token1, user3);
        followUserWithRequest(token1, user4);

        var feed1 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoriesFeedEntry.class);

        assertThat(feed1).isEmpty();
    }

    @Test
    void emptyStoriesFeedIsStillCachedAfterFirstFetchAttempt() {
        followUserWithRequest(token1, user2);
        followUserWithRequest(token1, user3);
        followUserWithRequest(token1, user4);

        var feed1 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoriesFeedEntry.class);

        var feed2 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoriesFeedEntry.class);

        assertThat(feed1).isEmpty();
        assertThat(feed2).isEmpty();
    }

    @Test
    void getStoriesFeedIsEmptyWhenNoOneIsFollowed() {
        var feed1 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoriesFeedEntry.class);

        assertThat(feed1).isEmpty();
    }

    @Test
    void getStoriesFeedWhenNotFoundInCache() 
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        
        followUserWithRequest(token1, user2);
        followUserWithRequest(token1, user3);
        followUserWithRequest(token1, user4);

        createStoryForUser(token2);
        createStoryForUser(token3);

        var storiesFeedEntryUser2 = StoriesFeedEntry.builder()
                .username(user2.getUsername())
                .profilePictureUrl(user2.getProfilePictureUrl())
                .build();
        var storiesFeedEntryUser3 = StoriesFeedEntry.builder()
                .username(user3.getUsername())
                .profilePictureUrl(user3.getProfilePictureUrl())
                .build();

        waitForAsyncStoryCache();
        
        deleteStoriesFeedFromCacheForUser(user1);
        
        var feed1 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoriesFeedEntry.class);

        assertThat(feed1).hasSize(2);
        assertThat(feed1.get(0)).isEqualTo(storiesFeedEntryUser2);
        assertThat(feed1.get(1)).isEqualTo(storiesFeedEntryUser3);
    }

    private void deleteStoriesFeedFromCacheForUser(User user)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        var constructKeyMethod = StoryRedisService.class.getDeclaredMethod("constructStoriesFeedKey", Long.class);
        constructKeyMethod.setAccessible(true);
        var key = (String) constructKeyMethod.invoke(storyRedisService, user.getId());
        redisTemplate.delete(key);
    }

    // TODO: TEST FOR MODIFYING ALREADY CREATED STORIES
}