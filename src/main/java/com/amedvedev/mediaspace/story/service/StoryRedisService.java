package com.amedvedev.mediaspace.story.service;

import com.amedvedev.mediaspace.story.Story;
import com.amedvedev.mediaspace.story.StoryMapper;
import com.amedvedev.mediaspace.story.dto.StoriesFeedEntry;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    public void cacheStoryDto(StoryDto storyDto) {
        var id = storyDto.getId();
        log.debug("Caching story with id: {}", id);

        var key = STORY_PREFIX + id;
        redisTemplate.opsForValue().set(key, storyDto);
    }

    public Optional<StoryDto> getStoryDtoById(Long id) {
        log.debug("Retrieving story with id: {}", id);
        var key = STORY_PREFIX + id;
        return Optional.ofNullable((StoryDto) redisTemplate.opsForValue().get(key));
    }

    public void removeStoryDtoById(Long id) {
        log.debug("Removing story with id: {}", id);
        var key = STORY_PREFIX + id;
        redisTemplate.delete(key);
    }

    public void cacheStoriesFeedByUserId(Long id, Set<StoriesFeedEntry> storiesFeedEntries) {
        log.debug("Caching stories feed for user with id: {}", id);
        var key = constructStoriesFeedKey(id);
        
        var tuples = storyMapper.mapStoriesFeedProjectionsToTuples(storiesFeedEntries);
        redisTemplate.opsForZSet().add(key, tuples);
    }

    public List<StoriesFeedEntry> getStoriesFeedByUserId(Long id) {
        log.debug("Retrieving stories feed for user with id: {}", id);
        var key = constructStoriesFeedKey(id);
        System.out.println("LOOKING FOR KEY " + key);
        var storiesFeedProjections = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
        System.out.println("STORIES FEED PROJECTIONS: " + storiesFeedProjections);
        return storiesFeedProjections == null ? List.of() : storyMapper.mapTuplesToStoriesFeed(storiesFeedProjections);
    }
    
    public void cacheStoriesIdsForUser(Long userId, List<Long> storiesIds) {
        log.debug("Caching stories ids for user with id: {}", userId);
        var key = constructStoriesKey(userId);

        var storiesIdsTuples = getStoriesIdsTuples(storiesIds);
        redisTemplate.opsForZSet().add(key, storiesIdsTuples);
    }

    public List<Long> getStoriesIdsForUser(Long userId) {
        log.debug("Retrieving stories ids for user with id: {}", userId);
        var key = constructStoriesKey(userId);
        
        var storiesIds = redisTemplate.opsForZSet().reverseRange(key, 0, -1);
        return storiesIds == null ? List.of() : storyMapper.mapStoriesIdsObjectsToLong(storiesIds);
    }

    public void cacheStoryIdToUserStories(Long userId, Story story) {
        var storyId = story.getId();
        log.debug("Caching story with id {} to user with id {}", storyId, userId);

        var key = constructStoriesKey(userId);
        redisTemplate.opsForZSet().add(key, storyId, story.getCreatedAt().toEpochMilli());
    }

    public void removeStoryIdFromUserStories(Long userId, Long storyId) {
        log.debug("Removing story with id {} from user with id {}", storyId, userId);
        var key = constructStoriesKey(userId);
        redisTemplate.opsForZSet().remove(key, storyId);
    }
    
    public void addFeedEntryToFollowersFeed(Long publisherId, StoriesFeedEntry feedEntry, List<Long> followersIds) {
        log.debug("Caching story publisher id {} to followers feeds", publisherId);

        if (followersIds.isEmpty()) {
            log.debug("No followers of user with id {} found", publisherId);
            return;
        }
        
        // KEYS [user:3, story:1, story:3, user:3:stories, user:1, story:2, user:2, user:4, user:2:stories]
        System.out.println("FOLLOWERS IDS: " + followersIds);
        redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            followersIds.forEach(id -> {
                var key = constructStoriesFeedKey(id);
                System.out.println("ADDING TO ZSET WITH KEY " + key);
                addToRedisSortedSet(key, feedEntry, connection);
            });
            return null;
        });
    }

    private void addToRedisSortedSet(String key, Object value, RedisConnection connection) {
        var keyBytes = redisTemplate.getStringSerializer().serialize(key);
        
        // For some reason redisTemplate.getValueSerializer().serialize() doesnt work, although set in RedisConfig
        var valueSerializer = new GenericJackson2JsonRedisSerializer();
        var valueBytes = valueSerializer.serialize(value);

        if (keyBytes != null && valueBytes != null) {
            connection.zAdd(keyBytes, Instant.now().toEpochMilli(), valueBytes);
        } else {
            log.error("Serialization resulted in null value for key {} or value {}", key, value);
        }
    }

    private Set<ZSetOperations.TypedTuple<Object>> getStoriesIdsTuples(List<Long> ids) {
        return ids.stream()
                .map(id -> new DefaultTypedTuple<>((Object) id, (double) Instant.now().toEpochMilli()))
                .collect(Collectors.toSet());
    }

    private String constructStoriesFeedKey(Long userId) {
        return USER_PREFIX + userId + STORIES_FEED_SUFFIX;
    }

    private String constructStoriesKey(Long userId) {
        return USER_PREFIX + userId + STORIES_SUFFIX;
    }
}
