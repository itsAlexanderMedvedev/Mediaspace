package com.amedvedev.mediaspace.redis;

import com.amedvedev.mediaspace.story.dto.ViewStoriesFeedResponse;
import com.amedvedev.mediaspace.user.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String STORIES_FEED_KEY_PREFIX = "stories_feed:";
    private static final String USER_KEY_PREFIX = "user:";

    public void cacheStoriesFeedForUser(Long id, List<ViewStoriesFeedResponse> stories) {
        log.debug("Caching stories feed for user with id: {}", id);
        String key = STORIES_FEED_KEY_PREFIX + id;
        try {
            String storiesJson = objectMapper.writeValueAsString(stories);
            redisTemplate.opsForValue().set(key, storiesJson);
            log.debug("Cached stories feed for user with id: {}", id);
        } catch (JsonProcessingException e) {
            log.error("Error serializing stories feed for user with id: {}", id);
        }
    }

    public List<ViewStoriesFeedResponse> getCachedStoriesFeedForUser(Long id) {
        log.debug("Retrieving stories feed from cache for user with id: {}", id);

        String key = STORIES_FEED_KEY_PREFIX + id;
        String storiesJson = redisTemplate.opsForValue().get(key);

        if (storiesJson != null) {
            try {
                objectMapper.readerForListOf(ViewStoriesFeedResponse.class);
                return objectMapper.readValue(storiesJson, new TypeReference<List<ViewStoriesFeedResponse>>() {});
            } catch (JsonProcessingException e) {
                log.error("Error deserializing stories feed from cache for user with id: {}", id, e);
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    public void clearCachedStoriesFeedForUser(Long userId) {
        String key = STORIES_FEED_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.debug("Cleared cached stories feed for user with id: {}", userId);
    }

    public void cacheUser(User user) {
        log.debug("Caching user with id: {}", user.getId());
        String key = USER_KEY_PREFIX + user.getId();
        try {
            String userJson = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(key, userJson);
            log.debug("Cached user with id: {}", user.getId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing user with id: {}", user.getId());
        }
    }

    public Optional<User> getCachedUser(String username) {
        log.debug("Retrieving user from cache with id: {}", username);

        String key = USER_KEY_PREFIX + username;
        String userJson = redisTemplate.opsForValue().get(key);

        if (userJson != null) {
            try {
                return Optional.of(objectMapper.readValue(userJson, User.class));
            } catch (JsonProcessingException e) {
                log.error("Error deserializing user from cache with username: {}", username, e);
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public void clearCachedUser(Long id) {
        String key = USER_KEY_PREFIX + id;
        redisTemplate.delete(key);
        log.debug("Cleared cached user data for user with id: {}", id);
    }
}
