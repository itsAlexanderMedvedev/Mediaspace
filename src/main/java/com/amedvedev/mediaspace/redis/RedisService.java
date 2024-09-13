package com.amedvedev.mediaspace.redis;

import com.amedvedev.mediaspace.story.dto.ViewStoriesFeedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private static final String STORIES_FEED_KEY_PREFIX = "stories_feed:";

    public List<ViewStoriesFeedResponse> getCachedStoriesFeedForUser(Long id) {
        String key = STORIES_FEED_KEY_PREFIX + id;
        String cachedStories = redisTemplate.opsForValue().get(key);

        if (cachedStories != null) {
            try {
                log.info("Retrieving stories feed from cache for user with id: {}", id);
                objectMapper.readerForListOf(ViewStoriesFeedResponse.class);
                return objectMapper.readValue(cachedStories, new TypeReference<List<ViewStoriesFeedResponse>>() {});
            } catch (JsonProcessingException e) {
                log.error("Error deserializing stories feed from cache for user with id: {}", id);
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    public void cacheStoriesFeedForUser(Long id, List<ViewStoriesFeedResponse> stories) {
        log.info("Caching stories feed for user with id: {}", id);
        String key = STORIES_FEED_KEY_PREFIX + id;
        try {
            String storiesJson = objectMapper.writeValueAsString(stories);
            redisTemplate.opsForValue().set(key, storiesJson);
            log.info("Cached stories feed for user with id: {}", id);
        } catch (JsonProcessingException e) {
            log.error("Error serializing stories feed for user with id: {}", id);
        }
    }
}
