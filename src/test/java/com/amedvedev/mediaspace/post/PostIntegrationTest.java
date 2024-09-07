package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.auth.JwtService;
import com.amedvedev.mediaspace.testutils.AbstractIntegrationTest;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PostIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private Integer port;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtService jwtService;

    private User user;

    private String token;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "/api/posts";

        userRepository.deleteAll();

        user = userRepository.save(User.builder().username("user").password("encoded-password").build());
        token = jwtService.generateToken(user);
    }
}
