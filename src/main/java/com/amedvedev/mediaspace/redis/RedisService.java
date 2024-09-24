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
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String STORIES_FEED_KEY_PREFIX = "stories_feed:";

    public void cacheStoriesFeedForUser(Long id, List<ViewStoriesFeedResponse> stories) {
        log.debug("Caching stories feed for user with id: {}", id);
        String key = STORIES_FEED_KEY_PREFIX + id;
        try {
            String storiesJson = objectMapper.writeValueAsString(stories);
            redisTemplate.opsForValue().set(key, storiesJson);
            log.debug("Cached stories feed for user with id: {}", id);
        } catch (JsonProcessingException e) {
            log.error("Error serializing stories feed for user with id: {}", id, e);
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
}
