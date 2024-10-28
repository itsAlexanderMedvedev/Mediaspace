package com.amedvedev.mediaspace.testutil;

import com.redis.testcontainers.RedisContainer;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.Callable;

public abstract class AbstractIntegrationTest {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String CLEAR_DB = "TRUNCATE story, comment, post, media, _user RESTART IDENTITY CASCADE";

    @Autowired
    protected JdbcTemplate jdbcTemplate;
    
    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    protected static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
                .withUsername("postgres")
                .withPassword("pass")
                .withDatabaseName("mediaspace");

    protected static RedisContainer redis = new RedisContainer(
            RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    private static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
    }

    // This method is needed for the case where the test is run with @Transactional
    // and if we want to insert some data into the database we interrupt the transaction,
    // insert data and then start a new transaction 
    protected <T> T executeInsideTransaction(Callable<T> callable) {
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

    protected void clearDbAndRedis() {
        executeInsideTransaction(() -> {
            jdbcTemplate.execute(CLEAR_DB);
            return null;
        });

        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }
}
