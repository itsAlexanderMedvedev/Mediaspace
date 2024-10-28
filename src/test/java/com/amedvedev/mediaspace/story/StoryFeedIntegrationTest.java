package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.story.dto.StoryPreviewResponse;
import com.amedvedev.mediaspace.story.service.StoryRedisService;
import com.amedvedev.mediaspace.testutil.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserRepository;
import com.amedvedev.mediaspace.user.follow.FollowRepository;
import com.amedvedev.mediaspace.user.service.UserService;
import io.lettuce.core.output.VoidOutput;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StoryFeedIntegrationTest extends AbstractIntegrationTest {

    private static final String STORIES_FEED_ENDPOINT = "/api/feed/stories";
    private static final String FOLLOW_ENDPOINT = "/api/users/{username}/follow";

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

    private Story createStoryForUser(User user) {
        var media = Media.builder().url("https://example.com").build();
        return executeInsideTransaction(() -> storyRepository.save(Story.builder().user(user).media(media).build()));
    }

    private void followUserWithRequest(String token, User followee) {
        given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .when()
                .post(FOLLOW_ENDPOINT, followee.getUsername())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void getStoriesFeed() {
        followUserWithRequest(token1, user2);
        followUserWithRequest(token1, user3);
        followUserWithRequest(token1, user4); // user4 is not posting anything
        followUserWithRequest(token3, user2); // user3 is following user2, should not affect user1's feed

        var story1 = createStoryForUser(user3);
        var story2 = createStoryForUser(user3);
        var story3 = createStoryForUser(user2);

        var feed1 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoryPreviewResponse.class);

        assertThat(feed1).hasSize(3);
        assertThat(feed1.get(0)).isEqualTo(storyMapper.toStoryPreviewResponse(story3));
        assertThat(feed1.get(1)).isEqualTo(storyMapper.toStoryPreviewResponse(story2));
        assertThat(feed1.get(2)).isEqualTo(storyMapper.toStoryPreviewResponse(story1));
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
                .getList(".", StoryPreviewResponse.class);

        assertThat(feed1).isEmpty();
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
                .getList(".", StoryPreviewResponse.class);

        assertThat(feed1).isEmpty();
    }
    
    @Test
    void getStoriesFeedIsTheSameWhenGotFromCache() {
        followUserWithRequest(token1, user2);
        followUserWithRequest(token1, user3);
        followUserWithRequest(token1, user4);
        
        var story1 = createStoryForUser(user3);
        var story2 = createStoryForUser(user3);
        var story3 = createStoryForUser(user2);

        // nothing is cached yet
        var feed1 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoryPreviewResponse.class);

        assertThat(feed1).hasSize(3);
        assertThat(feed1.get(0)).isEqualTo(storyMapper.toStoryPreviewResponse(story3));
        assertThat(feed1.get(1)).isEqualTo(storyMapper.toStoryPreviewResponse(story2));
        assertThat(feed1.get(2)).isEqualTo(storyMapper.toStoryPreviewResponse(story1));
        
        // only stories ids are cached
        var feed2 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoryPreviewResponse.class);

        assertThat(feed2).hasSize(3);
        assertThat(feed2.get(0)).isEqualTo(storyMapper.toStoryPreviewResponse(story3));
        assertThat(feed2.get(1)).isEqualTo(storyMapper.toStoryPreviewResponse(story2));
        assertThat(feed2.get(2)).isEqualTo(storyMapper.toStoryPreviewResponse(story1));
        
        // stories ids along with stories are cached
        var feed3 = given()
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token1)
                .when()
                .get(STORIES_FEED_ENDPOINT)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .jsonPath()
                .getList(".", StoryPreviewResponse.class);
        
        assertThat(feed3).hasSize(3);
        assertThat(feed3.get(0)).isEqualTo(storyMapper.toStoryPreviewResponse(story3));
        assertThat(feed3.get(1)).isEqualTo(storyMapper.toStoryPreviewResponse(story2));
        assertThat(feed3.get(2)).isEqualTo(storyMapper.toStoryPreviewResponse(story1));
    }
    
    // TODO: TEST FOR MODIFYING ALREADY CREATED STORIES
}