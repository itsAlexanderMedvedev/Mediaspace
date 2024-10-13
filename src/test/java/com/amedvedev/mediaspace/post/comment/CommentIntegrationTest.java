package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.post.PostMapper;
import com.amedvedev.mediaspace.post.PostRepository;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.EditCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.ViewPostCommentsResponse;
import com.amedvedev.mediaspace.testutil.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CommentIntegrationTest extends AbstractIntegrationTest {

    public static final String COMMENTS_ENDPOINT = "/api/comments";
    public static final String POST_ID_ENDPOINT = "/posts/{postId}";
    public static final String COMMENT_ID_ENDPOINT = "/{commentId}";


    @LocalServerPort
    private Integer port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentRepository commentRepository;

    private User user;
    private Post post;
    private String token;
    @Autowired
    private PostMapper postMapper;


    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = COMMENTS_ENDPOINT;

        clearDbAndFlushRedis();

        user = createUser("user");
        post = createPost(user, "title", "description");

        token = jwtService.generateToken(user);
    }

    private User createUser(String username) {
        return executeInsideTransaction(() -> {
            var user = User.builder().username(username).password("encoded-password").build();
            return userRepository.save(user);
        });
    }

    private Post createPost(User user, String title, String description) {
        return executeInsideTransaction(() -> {
            var post = Post.builder()
                    .user(user)
                    .title(title)
                    .description(description)
                    .build();

            return postRepository.save(post);
        });
    }

    private void addCommentsToPost(User user, Post post, String... bodies) {
        executeInsideTransaction(() -> {
            for (String body : bodies) {
                var comment = Comment.builder()
                        .user(user)
                        .post(post)
                        .body(body)
                        .build();

                commentRepository.save(comment);
            }
            return null;
        });
    }

    private void addNestedCommentToComment(User user, Long commentId, String... bodies) {
        executeInsideTransaction(() -> {
            var parentComment = commentRepository.findById(commentId).orElseThrow();

            for (String body : bodies) {
                var nestedComment = Comment.builder()
                        .user(user)
                        .post(parentComment.getPost())
                        .body(body)
                        .parentComment(parentComment)
                        .build();

                commentRepository.save(nestedComment);
            }
            return null;
        });
    }

    @Test
    void shouldCreateComment() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, post.getId())
                .then()
                .log().all()
                .statusCode(HttpStatus.CREATED.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        var comments = updatedPost.getComments();
        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getBody()).isEqualTo("text");
    }

    @ParameterizedTest
    @MethodSource("getIncorrectCommentBodies")
    void shouldNotCreateCommentWhenBodyIsIncorrect(String body) {
        var commentRequest = AddCommentRequest.builder().body(body).build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());

        var targetedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(targetedPost.getComments()).isEmpty();
    }

    @Test
    void shouldCreateNestedComments() {
        addCommentsToPost(user, post, "text");
        var commentRequest = AddCommentRequest.builder().body("nested text").build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .log().all()
                .statusCode(HttpStatus.CREATED.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        var comments = updatedPost.getComments();
        assertThat(comments).hasSize(1);

        var nestedComments = comments.getFirst().getNestedComments();
        assertThat(nestedComments).hasSize(1);
        assertThat(nestedComments.getFirst().getBody()).isEqualTo("nested text");
    }

    @Test
    void shouldNotCreateNestedCommentsIfCommentDoesNotExist() {
        var commentRequest = AddCommentRequest.builder().body("nested text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(COMMENT_ID_ENDPOINT, 1)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldViewComments() {
        addCommentsToPost(user, post, "text", "text2");

        var response = given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .get(POST_ID_ENDPOINT, post.getId())
                .then()
                .log().all()
                .statusCode(200)
                .extract()
                .as(ViewPostCommentsResponse.class);


        var comment1 = response.getComments().getFirst();
        var comment2 = response.getComments().get(1);

        assertThat(response.getComments().size()).isEqualTo(2);

        assertThat(comment1.getId()).isEqualTo(1L);
        assertThat(comment1.getBody()).isEqualTo("text");
        assertThat(comment1.getAuthor()).isEqualTo("user");

        assertThat(comment2.getId()).isEqualTo(2L);
        assertThat(comment2.getBody()).isEqualTo("text2");
        assertThat(comment2.getAuthor()).isEqualTo("user");
    }

    @Test
    void shouldNotViewCommentsIfPostDoesNotExist() {
        var notExistingPostId = 2;
        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .get(POST_ID_ENDPOINT, notExistingPostId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldNotViewCommentsIfUserIsNotAuthenticated() {
        addCommentsToPost(user, post, "text", "text2");

        given()
                .contentType(ContentType.JSON)
                .get(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void shouldViewNestedComments() {
        addCommentsToPost(user, post, "text");
        addNestedCommentToComment(user, 1L, "nested text");

        var response = given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .get(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .extract()
                .as(ViewPostCommentsResponse.class);

        var comment1 = response.getComments().getFirst();
        var nestedComment = comment1.getNestedComments().getFirst();

        assertThat(response.getComments().size()).isEqualTo(2);

        assertThat(comment1.getId()).isEqualTo(1L);
        assertThat(comment1.getBody()).isEqualTo("text");
        assertThat(comment1.getAuthor()).isEqualTo("user");

        assertThat(nestedComment.getId()).isEqualTo(2L);
        assertThat(nestedComment.getBody()).isEqualTo("nested text");
        assertThat(nestedComment.getAuthor()).isEqualTo("user");
    }

    @Test
    void shouldNotViewNestedCommentsIfCommentDoesNotExist() {
        addCommentsToPost(user, post, "text");

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .get(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("comments.size()", equalTo(1))
                .body("comments[0].nestedComments.size()", equalTo(0));
    }

    @Test
    void shouldEditComment() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value());

        var updatedCommentRequest = EditCommentRequest.builder().updatedBody("new text").build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(updatedCommentRequest)
                .patch(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .statusCode(HttpStatus.OK.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        var comments = updatedPost.getComments();
        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getBody()).isEqualTo("new text");
    }

    @ParameterizedTest
    @MethodSource("getIncorrectCommentBodies")
    void shouldNotEditCommentWhenBodyIsIncorrect(String newBody) {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value());

        var updatedCommentRequest = EditCommentRequest.builder().updatedBody(newBody).build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(updatedCommentRequest)
                .patch(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        var comments = updatedPost.getComments();

        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getBody()).isEqualTo("text");
    }

    private static String[] getIncorrectCommentBodies() {
        return new String[] {
                "a".repeat(2049),
                " ",
                null
        };
    }

    @Test
    void shouldNotCreateCommentIfUserIsNotAuthenticated() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getComments()).isEmpty();
    }

    @Test
    void shouldNotEditCommentIfUserIsNotAuthenticated() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value());

        var updatedCommentRequest = EditCommentRequest.builder().updatedBody("new text").build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .body(updatedCommentRequest)
                .patch(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        var comments = updatedPost.getComments();
        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getBody()).isEqualTo("text");
    }

    @Test
    void shouldNotAddCommentIfPostDoesNotExist() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, 2)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("reason", equalTo("Post not found"));;
    }

    @Test
    void shouldNotEditCommentIfCommentDoesNotExist() {
        var updatedCommentRequest = EditCommentRequest.builder().updatedBody("new text").build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(updatedCommentRequest)
                .patch(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldNotEditCommentsOfOtherUsers() {
        addCommentsToPost(user, post, "text");
        var commentId = 1;

        var anotherUser = createUser("anotherUser");
        var anotherToken = jwtService.generateToken(anotherUser);

        var editCommentRequest = EditCommentRequest.builder().updatedBody("new text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + anotherToken)
                .body(editCommentRequest)
                .patch(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        var comments = updatedPost.getComments();

        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getBody()).isEqualTo("text");
    }

    @Test
    void shouldNotAllowUnauthorizedUserToEditComment() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value());

        var updatedCommentRequest = EditCommentRequest.builder().updatedBody("new text").build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .body(updatedCommentRequest)
                .patch(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        var comments = updatedPost.getComments();
        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getBody()).isEqualTo("text");
    }

    @Test
    void shouldDeleteComment() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value());

        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .delete(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getComments()).isEmpty();
    }

    @Test
    void shouldNotDeleteCommentIfUserIsNotAuthenticated() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .body(commentRequest)
                .post(POST_ID_ENDPOINT, post.getId())
                .then()
                .statusCode(HttpStatus.CREATED.value());

        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .delete(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        var comments = updatedPost.getComments();
        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getBody()).isEqualTo("text");
    }

    @Test
    void shouldNotDeleteCommentIfCommentDoesNotExist() {
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                .delete(COMMENT_ID_ENDPOINT, commentId)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}