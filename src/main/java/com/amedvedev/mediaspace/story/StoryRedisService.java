package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.story.dto.StoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryMapper storyMapper;

    private static final String STORY_PREFIX = "story:";
    private static final String USER_PREFIX = "user:";
    private static final String STORIES_FEED_SUFFIX = ":stories_feed";
    private static final String STORIES_SUFFIX = ":stories";

    public void cacheStoryDto(Long id, StoryDto storyDto) {
        log.debug("Caching story with id: {}", id);
        var key = STORY_PREFIX + id;
        redisTemplate.opsForValue().set(key, storyDto);
    }

    public void cacheStoriesIdsForUser(Long userId, List<Story> stories) {
        log.debug("Caching stories ids for user with id: {}", userId);
        var key = constructStoriesKey(userId);
        var storiesIdsTuples = getStoriesIdsTuples(stories);
        redisTemplate.opsForZSet().add(key, storiesIdsTuples);
    }

    public void cacheStoryIdToUserStories(Long userId, Story story) {
        var storyId = story.getId();
        log.debug("Caching story with id {} to user with id {}", storyId, userId);
        var key = constructStoriesKey(userId);
        redisTemplate.opsForZSet().add(key, storyId, story.getCreatedAt().toEpochMilli());
    }

    public List<Long> getStoriesIdsForUser(Long userId) {
        log.debug("Retrieving stories ids for user with id: {}", userId);
        var key = constructStoriesKey(userId);
        redisTemplate.opsForValue().set("testkey", "testvalue");
        var storiesIds = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
        return storiesIds == null ? List.of() : storyMapper.mapStoriesIdsObjectsToLong(storiesIds);
    }

    public void removeStoryIdFromUserStories(Long userId, Long storyId) {
        log.debug("Removing story with id {} from user with id {}", storyId, userId);
        var key = constructStoriesKey(userId);
        redisTemplate.opsForZSet().remove(key, storyId);
    }

    public void addStoryIdToFollowersFeed(List<Long> followersIds, Story story) {
        var storyId = story.getId();
        log.debug("Caching story id {} to followers feeds", storyId);

        if (followersIds.isEmpty()) {
            log.debug("No followers to add story to");
            return;
        }

        var storyCreationTimestamp = story.getCreatedAt().toEpochMilli();
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

    public void cacheFeedStoriesIdsForUser(Long id, List<Story> stories) {
        log.debug("Caching feed stories ids for user with id: {}", id);
        var key = constructStoriesFeedKey(id);
        var storiesIdsTuples = getStoriesIdsTuples(stories);
        redisTemplate.opsForZSet().add(key, storiesIdsTuples);
    }

    public List<Long> getFeedStoriesIdForUser(Long userId) {
        var key = constructStoriesFeedKey(userId);
        var storiesIdsObjects = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
        return storiesIdsObjects == null ? List.of() : storyMapper.mapStoriesIdsObjectsToLong(storiesIdsObjects);
    }

    private Set<ZSetOperations.TypedTuple<Object>> getStoriesIdsTuples(List<Story> stories) {
        return stories.stream()
                .map(story -> new DefaultTypedTuple<>((Object) story.getId(), (double) story.getCreatedAt().toEpochMilli()))
                .collect(Collectors.toSet());
    }

    public StoryDto getStoryDtoById(Long id) {
        var key = STORY_PREFIX + id;
        return (StoryDto) redisTemplate.opsForValue().get(key);
    }

    public void removeStoryDtoById(Long id) {
        var key = STORY_PREFIX + id;
        redisTemplate.delete(key);
    }

    private String constructStoriesFeedKey(Long userId) {
        return USER_PREFIX + userId + STORIES_FEED_SUFFIX;
    }

    private String constructStoriesKey(Long userId) {
        return USER_PREFIX + userId + STORIES_SUFFIX;
    }
}
