package com.amedvedev.mediaspace.testutil;

import com.redis.testcontainers.RedisContainer;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
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

import javax.swing.text.html.parser.Entity;
import java.util.concurrent.Callable;

public abstract class AbstractIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;
    
    @Autowired
    protected EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;


    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"))
                .withUsername("postgres")
                .withPassword("pass")
                .withDatabaseName("mediaspace");

    static RedisContainer redis = new RedisContainer(
            RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.liquibase.url", postgres::getJdbcUrl);
        registry.add("spring.liquibase.user", postgres::getUsername);
        registry.add("spring.liquibase.password", postgres::getPassword);

        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getRedisPort);
    }

    protected  <T> T executeInsideTransaction(Callable<T> callable) {
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
}
