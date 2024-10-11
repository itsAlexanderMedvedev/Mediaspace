package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.story.dto.StoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String STORIES_FEED_KEY_PREFIX = "user:";
    private static final String STORIES_FEED_KEY_SUFFIX = ":stories";

//    public void cacheStoriesFeedForUser(Long id, List<ViewStoriesFeedResponse> stories) {
//        log.debug("Caching stories feed for user with id: {}", id);
//        var key = STORIES_FEED_KEY_PREFIX + id;
//        String storiesJson = objectMapper.writeValueAsString(stories);
//        redisTemplate.opsForValue().set(key, storiesJson);
//    }
//
//    public List<ViewStoriesFeedResponse> getCachedStoriesFeedForUser(Long id) {
//        log.debug("Retrieving stories feed from cache for user with id: {}", id);
//
//        String key = STORIES_FEED_KEY_PREFIX + id;
//        String storiesJson = redisTemplate.opsForValue().get(key);
//
//        if (storiesJson != null) {
//            try {
//                objectMapper.readerForListOf(ViewStoriesFeedResponse.class);
//                return objectMapper.readValue(storiesJson, new TypeReference<List<ViewStoriesFeedResponse>>() {});
//            } catch (JsonProcessingException e) {
//                log.error("Error deserializing stories feed from cache for user with id: {}", id, e);
//                return new ArrayList<>();
//            }
//        }
//        return new ArrayList<>();
//    }
//
//    public void clearCachedStoriesFeedForUser(Long userId) {
//        String key = STORIES_FEED_KEY_PREFIX + userId;
//        redisTemplate.delete(key);
//        log.debug("Cleared cached stories feed for user with id: {}", userId);
//    }

    public void cacheStoryDto(Long id, StoryDto storyDto) {
        log.debug("Caching story with id: {}", id);
        var key = STORIES_FEED_KEY_PREFIX + id;
        redisTemplate.opsForValue().set(key, storyDto);
    }

    public void addStoryIdToFollowersFeed(List<Long> followersIds, StoryDto storyDto) {
        log.debug("Adding story with id {} to followers feeds", storyDto.getId());

        if (followersIds.isEmpty()) {
            log.debug("No followers to add story to");
            return;
        }

        var storyCreationTimestamp = storyDto.getCreatedAt().toEpochMilli();
        var storyId = storyDto.getId();
        redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            followersIds.forEach(id -> addToRedisSortedSet(storyId, id, connection, storyCreationTimestamp));
            return null;
        });
    }

    private void addToRedisSortedSet(Long storyId,
                                     Long followerId,
                                     RedisConnection connection,
                                     long storyCreationTimestamp) {

        var key = constructStoriesFeedKey(followerId);
        var keyBytes = redisTemplate.getStringSerializer().serialize(key);
        var valueBytes = redisTemplate.getStringSerializer().serialize(storyId.toString());
        if (keyBytes != null && valueBytes != null) {
            connection.zAdd(keyBytes, storyCreationTimestamp, valueBytes);
        } else {
            log.error("Serialization resulted in null value for key {} or value {}", key, storyId);
        }
    }

    private String constructStoriesFeedKey(Long id) {
        return STORIES_FEED_KEY_PREFIX + id + STORIES_FEED_KEY_SUFFIX;
    }

    public Object getCachedStoriesFeedForUser(Long id) {
    }
}
