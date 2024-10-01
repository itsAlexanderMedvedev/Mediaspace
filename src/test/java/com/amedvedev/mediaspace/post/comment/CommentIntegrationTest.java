package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.post.PostMapper;
import com.amedvedev.mediaspace.post.PostRepository;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.EditCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.ViewCommentResponse;
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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CommentIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlatformTransactionManager transactionManager;

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
//    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/comments";

//        TestTransaction.end();
        executeInsideTransaction(() -> {
            jdbcTemplate.execute("TRUNCATE comment, post, _user RESTART IDENTITY CASCADE");

            user = createUser("user");
            post = createPost(user, "title", "description");

            return null;
        });
//        TestTransaction.start();

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

    private void addCommentToPost(User user, Post post, String... bodies) {
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

    private <T> T executeInsideTransaction(Callable<T> callable) {
        // BeforeEach is executed inside the same transaction as the test method.
        // That causes the entities that we create here to be locked due to the isolation level READ_COMMITTED and above.
        // This is why we end the test transaction here and start a new one after creating entities.

        System.out.println("callable " + TransactionSynchronizationManager.isActualTransactionActive());

        var transactionExisted = TestTransaction.isActive();
        if (transactionExisted) TestTransaction.end();

        TransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            T result = callable.call();
            transactionManager.commit(status);
            return result;
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw new RuntimeException(e);
        } finally {
            if (transactionExisted) TestTransaction.start();
        }
    }

    @Test
    void shouldCreateComment() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/posts/{postId}", post.getId())
                .then()
                .log().all()
                .statusCode(201);

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
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/posts/{postId}", post.getId())
                .then()
                .statusCode(400);

        var targetedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(targetedPost.getComments()).isEmpty();
    }

    @Test
    void shouldCreateNestedComments() {
        addCommentToPost(user, post, "text");

        var commentRequest = AddCommentRequest.builder().body("nested text").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/{commentId}", 1)
                .then()
                .statusCode(201);

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
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/{commentId}", 1)
                .then()
                .statusCode(404);
    }

    @Test
    void shouldViewComments() {
        addCommentToPost(user, post, "text", "text2");

        var response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .get("/posts/{postId}", post.getId())
                .then()
                .log().all()
                .statusCode(200)
                .extract()
                .as(ViewPostCommentsResponse.class);


        var comment1 = response.getComments().get(0);
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
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .get("/posts/{postId}", 2)
                .then()
                .statusCode(404);
    }

    @Test
    void shouldNotViewCommentsIfUserIsNotAuthenticated() {
        addCommentToPost(user, post, "text", "text2");

        given()
                .contentType(ContentType.JSON)
                .get("/posts/{postId}", post.getId())
                .then()
                .statusCode(401);
    }

    @Test
    void shouldViewNestedComments() {
        addCommentToPost(user, post, "text");
        addNestedCommentToComment(user, 1L, "nested text");

        var response = given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .get("/posts/{postId}", post.getId())
                .then()
                .statusCode(200)
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
        addCommentToPost(user, post, "text");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .get("/posts/{postId}", post.getId())
                .then()
                .statusCode(200)
                .body("comments.size()", equalTo(1))
                .body("comments[0].nestedComments.size()", equalTo(0));
    }

    @Test
    void shouldEditComment() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/posts/{postId}", post.getId())
                .then()
                .statusCode(201);

        var updatedCommentRequest = EditCommentRequest.builder().updatedBody("new text").build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updatedCommentRequest)
                .patch("/{commentId}", commentId)
                .then()
                .statusCode(200);

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
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/posts/{postId}", post.getId())
                .then()
                .statusCode(201);

        var updatedCommentRequest = EditCommentRequest.builder().updatedBody(newBody).build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updatedCommentRequest)
                .patch("/{commentId}", commentId)
                .then()
                .statusCode(400);

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
                .post("/posts/{postId}", post.getId())
                .then()
                .statusCode(401);

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getComments()).isEmpty();
    }

    @Test
    void shouldNotEditCommentIfUserIsNotAuthenticated() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/posts/{postId}", post.getId())
                .then()
                .statusCode(201);

        var updatedCommentRequest = EditCommentRequest.builder().updatedBody("new text").build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .body(updatedCommentRequest)
                .patch("/{commentId}", commentId)
                .then()
                .statusCode(401);

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
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/posts/{postId}", 2)
                .then()
                .statusCode(404)
                .body("reason", equalTo("Post not found"));;
    }

    @Test
    void shouldNotEditCommentIfCommentDoesNotExist() {
        var updatedCommentRequest = EditCommentRequest.builder().updatedBody("new text").build();
        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(updatedCommentRequest)
                .patch("/{commentId}", commentId)
                .then()
                .statusCode(404);
    }

    @Test
    void shouldNotEditCommentsOfOtherUsers() {
        addCommentToPost(user, post, "text");
        var commentId = 1;

        var anotherUser = createUser("anotherUser");
        var anotherToken = jwtService.generateToken(anotherUser);

        var editCommentRequest = EditCommentRequest.builder().updatedBody("new text").build();

        System.out.println("test " + user.getId() + " " + anotherUser.getId());

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + anotherToken)
                .body(editCommentRequest)
                .patch("/{commentId}", commentId)
                .then()
                .statusCode(403);

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
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/posts/{postId}", post.getId())
                .then()
                .statusCode(201);

        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .delete("/{commentId}", commentId)
                .then()
                .statusCode(204);

        var updatedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updatedPost.getComments()).isEmpty();
    }

    @Test
    void shouldNotDeleteCommentIfUserIsNotAuthenticated() {
        var commentRequest = AddCommentRequest.builder().body("text").build();

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(commentRequest)
                .post("/posts/{postId}", post.getId())
                .then()
                .statusCode(201);

        var commentId = 1;

        given()
                .contentType(ContentType.JSON)
                .delete("/{commentId}", commentId)
                .then()
                .statusCode(401);

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
                .header("Authorization", "Bearer " + token)
                .delete("/{commentId}", commentId)
                .then()
                .statusCode(404);
    }
}