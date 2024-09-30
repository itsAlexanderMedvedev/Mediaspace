package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.post.PostRepository;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.EditCommentRequest;
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
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

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

    private User user;
    private Post post;
    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/comments";

        // BeforeEach is executed inside the same transaction as the test method.
        // That causes the entities that we create here to be locked due to the isolation level READ_COMMITTED and above.
        // This is why we end the test transaction here and start a new one after creating entities.
        TestTransaction.end();

        TransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            jdbcTemplate.execute("TRUNCATE comment, post, _user RESTART IDENTITY CASCADE");

            user = createUser();
            post = createPost(user, "title", "description");

            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }

        token = jwtService.generateToken(user);

        TestTransaction.start();
    }

    private User createUser() {
        return userRepository.save(User.builder().username("user").password("encoded-password").build());
    }

    private Post createPost(User user, String title, String description) {
        var post = Post.builder()
                .user(user)
                .title(title)
                .description(description)
                .build();

        return postRepository.save(post);
    }

    @Test
    @Transactional
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

    @Test
    @Transactional
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
}