package com.amedvedev.mediaspace.redis;

import com.amedvedev.mediaspace.story.dto.ViewStoriesFeedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
                objectMapper.readerForListOf(ViewStoriesFeedResponse.class);
                return objectMapper.readValue(cachedStories, new TypeReference<List<ViewStoriesFeedResponse>>() {});
            } catch (JsonProcessingException e) {
                // TODO: Introduce logging
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }
}
