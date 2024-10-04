package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.media.MediaRepository;
import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
import com.amedvedev.mediaspace.media.dto.ViewPostMediaResponse;
import com.amedvedev.mediaspace.media.postmedia.PostMedia;
import com.amedvedev.mediaspace.media.postmedia.PostMediaId;
import com.amedvedev.mediaspace.media.postmedia.PostMediaRepository;
import com.amedvedev.mediaspace.post.comment.Comment;
import com.amedvedev.mediaspace.post.dto.CreatePostRequest;
import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.post.dto.ViewPostResponse;
import com.amedvedev.mediaspace.post.like.Like;
import com.amedvedev.mediaspace.post.like.LikeId;
import com.amedvedev.mediaspace.testutil.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hibernate.Session;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PostIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostMediaRepository postMediaRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private JwtService jwtService;

    private User user;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/posts";

        executeInsideTransaction(() -> {
            jdbcTemplate.execute("TRUNCATE post, media, _user RESTART IDENTITY CASCADE");
            user = createUser("user");
            return null;
        });

        token = jwtService.generateToken(user);
    }

    private User createUser(String username) {
        return userRepository.save(User.builder().username(username).password("encoded-password").build());
    }

    private Post createPost(String title, String description) {
        return executeInsideTransaction(() -> {
            var post = Post.builder()
                    .user(user)
                    .title(title)
                    .description(description)
                    .build();

            var mediaUrls = List.of("https://example.com/image.jpg", "https://example.com/video.mp4");

            var postMediaList = IntStream.range(0, mediaUrls.size())
                    .mapToObj(index -> {
                        var url = mediaUrls.get(index);
                        var media = Media.builder().url(url).build();
                        var mediaPostId = new PostMediaId(media.getId(), index + 1);
                        return new PostMedia(mediaPostId, media, post);
                    })
                    .toList();

            post.setPostMediaList(postMediaList);

            return postRepository.save(post);
        });
    }

    private Post addCommentsToPost(Post post, List<String> comments) {
        return executeInsideTransaction(() -> {
            comments.forEach(body -> {
                var comment = Comment.builder()
                        .body(body)
                        .post(post)
                        .user(user)
                        .build();

                post.getComments().add(comment);
            });

            return postRepository.save(post);
        });
    }

    private CreatePostRequest createPostRequest(String title, String description) {
        var mediaUrls = List.of(
                new CreateMediaRequest("https://example.com/image.jpg"),
                new CreateMediaRequest("https://example.com/video.mp4"));

        return CreatePostRequest.builder()
                .title(title)
                .description(description)
                .mediaUrls(mediaUrls)
                .build();
    }

    private ViewPostResponse getViewPostResponseExpected(String title, String description, Long id, Long[] mediaIds) {
        var viewPostMediaList = List.of(
                ViewPostMediaResponse.builder()
                .id(mediaIds[0])
                .url("https://example.com/image.jpg")
                .position(1)
                .build(),

                ViewPostMediaResponse.builder()
                .id(mediaIds[1])
                .url("https://example.com/video.mp4")
                .position(2)
                .build()
        );

        return ViewPostResponse.builder()
                .id(id)
                .username(user.getUsername())
                .title(title)
                .description(description)
                .postMediaList(viewPostMediaList)
                .build();
    }

    @Test
    void shouldNotCreatePostWithoutAuthorization() {
        given()
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("reason", equalTo("Full authentication is required to access this resource"));
    }

    @Test
    void shouldCreatePost() {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        var createPostRequest = createPostRequest("Title", "Hello, World!");
        var expectedViewPostResponse = getViewPostResponseExpected("Title", "Hello, World!", 1L, new Long[]{1L, 2L});

        var actualViewPostResponse = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(createPostRequest)
                .when()
                .post()
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .extract().jsonPath().getObject(".", ViewPostResponse.class);

        actualViewPostResponse.setCreatedAt(null);

        assertThat(expectedViewPostResponse).isEqualTo(actualViewPostResponse);
    }

    @Test
    void shouldReturnPostsOfUser(){
        createPost("Title1", "Hello, World!");
        createPost("Title2", "Hello, World!");

        var actualPostsList = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/{username}", "user")
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().jsonPath().getList(".", UserProfilePostResponse.class);

        assertThat(actualPostsList.size()).isEqualTo(2);
        assertThat(actualPostsList.get(0).getTitle()).isEqualTo("Title1");
        assertThat(actualPostsList.get(1).getTitle()).isEqualTo("Title2");
    }

    @Test
    void shouldNotAccessPostsOfUserWithInvalidUsername() {
        createPost("Title1", "Hello, World!");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/user/{username}", "invalid-username")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("User not found"));
    }

    @Test
    void shouldReturnPostById() {
        createPost("Title", "Hello, World!");

        var expectedViewPostResponse = getViewPostResponseExpected("Title", "Hello, World!", 1L, new Long[]{1L, 2L});

        var actualViewPostResponse = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/{id}", 1)
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract().jsonPath().getObject(".", ViewPostResponse.class);

        actualViewPostResponse.setCreatedAt(null);

        assertThat(expectedViewPostResponse).isEqualTo(actualViewPostResponse);
    }

    @Test
    void shouldNotAccessPostByIdWithInvalidId() {
        createPost("Title", "Hello, World!");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/{id}", 2)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("Post not found"));
    }

    @Test
    void shouldLikePost() {
        var post = createPost("Title", "Hello, World!");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .put("/{id}/like", post.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        var likedPost = postRepository.findById(post.getId()).orElseThrow();
        var expectedLike = Like.builder()
                .id(new LikeId(user.getId(), post.getId()))
                .user(user)
                .post(likedPost)
                .build();
        assertThat(likedPost.getLikes().size()).isEqualTo(1);
        assertThat(likedPost.getLikes()).contains(expectedLike);
    }

    @Test
    void shouldNotLikePostWithInvalidPostId() {
        createPost("Title", "Hello, World!");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .put("/{id}/like", 2)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("Post not found"));
    }

    @Test
    void shouldUnlikePost() {
        var post = createPost("Title", "Hello, World!");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .put("/{id}/like", post.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/{id}/like", post.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        var unlikedPost = postRepository.findById(post.getId()).orElseThrow();

        assertThat(unlikedPost.getLikes()).isEmpty();
    }

    @Test
    void shouldNotUnlikePostWithInvalidPostId() {
        createPost("Title", "Hello, World!");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/{id}/like", 2)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("Post not found"));
    }

    @Test
    void shouldNotUnlikePostIfNotLiked() {
        var post = createPost("Title", "Hello, World!");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/{id}/like", post.getId())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("reason", equalTo("Cannot unlike post that was not liked"));
    }

    @Test
    void shouldDeletePost() {
        createPost("Title", "Hello, World!");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/{id}", 1)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    void shouldDeleteMediaAndCommentsWhenDeletingPost() {
        var post = createPost("Title", "Hello, World!");
        var comments = List.of("Comment1", "Comment2", "Comment3");
        post = addCommentsToPost(post, comments);

        var mediaList = postMediaRepository.findAllByPostId(post.getId()).stream()
                .map(PostMedia::getMedia)
                .toList();

        System.out.println("POSTS: " + postRepository.findAll());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/{id}", post.getId())
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // TODO: look into that
        entityManager.flush();
        entityManager.clear();

        assertThat(postRepository.findById(post.getId())).isEmpty();
        assertThat(postMediaRepository.findAllByPostId(post.getId())).isEmpty();
        assertThat(mediaRepository.findAllById(mediaList.stream().map(Media::getId).toList())).isEmpty();
    }
}
