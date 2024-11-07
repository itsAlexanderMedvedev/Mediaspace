package com.amedvedev.mediaspace.feed;

import com.amedvedev.mediaspace.story.StoryMapper;
import com.amedvedev.mediaspace.story.dto.StoriesFeedEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryFeedRedisService {

    private static final String USER_PREFIX = "user:";
    private static final String STORIES_FEED_SUFFIX = ":stories_feed";
    private static final String EMPTY_FEED_MARKER = "EMPTY_FEED";
    private static final Optional<Set<StoriesFeedEntry>> EMPTY_FEED = Optional.of(Set.of());
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryMapper storyMapper;
    
    public void cacheStoriesFeedByUserId(Long id, Set<StoriesFeedEntry> storiesFeedEntries) {
        var key = constructStoriesFeedKey(id);

        if (storiesFeedEntries.isEmpty()) {
            log.debug("Adding empty feed marker for user with id: {}", id);
            redisTemplate.opsForZSet().add(key, EMPTY_FEED_MARKER, 0);
            return;
        }

        log.debug("Caching stories feed for user with id: {}", id);
        var tuples = storyMapper.mapStoriesFeedProjectionsToTuples(storiesFeedEntries);
        redisTemplate.opsForZSet().add(key, tuples);
    }

    public Optional<Set<StoriesFeedEntry>> getStoriesFeedByUserId(Long id) {
        log.debug("Looking for stories feed for user with id: {} in cache", id);
        var key = constructStoriesFeedKey(id);
        var storiesFeedProjectionsObjects = redisTemplate.opsForZSet().reverseRange(key, 0, -1);

        if (isFeedEmpty(storiesFeedProjectionsObjects)) {
            log.debug("Empty feed marker found for user with id: {}", id);
            return EMPTY_FEED;
        }

        if (feedIsNotInCache(storiesFeedProjectionsObjects)) {
            log.debug("No feed found in cache for user with id: {}", id);
            return Optional.empty();
        }

        return Optional.of(storyMapper.mapTuplesToStoriesFeed(storiesFeedProjectionsObjects));
    }

    private boolean feedIsNotInCache(Set<Object> storiesFeedProjectionsObjects) {
        return storiesFeedProjectionsObjects == null || storiesFeedProjectionsObjects.isEmpty();
    }

    private boolean isFeedEmpty(Set<Object> storiesFeedProjectionsObjects) {
        return storiesFeedProjectionsObjects != null
                && storiesFeedProjectionsObjects.size() == 1
                && storiesFeedProjectionsObjects.contains(EMPTY_FEED_MARKER);
    }

    public void cacheFeedEntryToFollowersFeeds(Long publisherId, StoriesFeedEntry feedEntry, List<Long> followersIds) {
        log.debug("Caching story publisher id {} to followers feeds", publisherId);

        if (followersIds.isEmpty()) {
            log.debug("No followers to add entry to of user with id {} found", publisherId);
            return;
        }
        
        redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            followersIds.forEach(id -> {
                var key = constructStoriesFeedKey(id);
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

    public void deleteFeedEntryFromFollowersFeeds(Long publisherId,
                                                  StoriesFeedEntry storiesFeedEntry,
                                                  List<Long> followersIds) {

        log.debug("Deleting story publisher id {} from followers feeds", publisherId);

        if (followersIds.isEmpty()) {
            log.debug("No followers to remove entry from of user with id {} found", publisherId);
            return;
        }

        redisTemplate.executePipelined((RedisCallback<?>) connection -> {
            followersIds.forEach(id -> {
                var key = constructStoriesFeedKey(id);
                var feedEntryBytes = new GenericJackson2JsonRedisSerializer().serialize(storiesFeedEntry);
                connection.zSetCommands().zRem(key.getBytes(), feedEntryBytes);
            });
            return null;
        });
    }

    
    private String constructStoriesFeedKey(Long userId) {
        return USER_PREFIX + userId + STORIES_FEED_SUFFIX;
    }
}
