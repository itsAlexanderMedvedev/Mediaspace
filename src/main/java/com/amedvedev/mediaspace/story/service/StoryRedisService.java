package com.amedvedev.mediaspace.story.service;

import com.amedvedev.mediaspace.story.Story;
import com.amedvedev.mediaspace.story.StoryMapper;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryRedisService {

    private static final String USER_PREFIX = "user:";
    private static final String STORY_PREFIX = "story:";
    private static final String STORIES_SUFFIX = ":stories";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final StoryMapper storyMapper;

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
        log.debug("Removing story dto with id: {}", id);
        var key = STORY_PREFIX + id;
        redisTemplate.delete(key);
    }

    public void cacheStoryIdToUserStories(Long userId, Story story) {
        var storyId = story.getId();
        log.debug("Caching story with id {} to user with id {}", storyId, userId);

        var key = constructStoriesKey(userId);
        System.out.println("Caching with score " + story.getCreatedAt().toEpochMilli());
        redisTemplate.opsForZSet().add(key, storyId, story.getCreatedAt().toEpochMilli());
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

        var storiesIds = getStoriesIdsInRangeFromYesterdayToTomorrow(key);
        return storiesIds == null ? List.of() : storyMapper.mapStoriesIdsObjectsToLong(storiesIds);
    }

    private Set<Object> getStoriesIdsInRangeFromYesterdayToTomorrow(String key) {
        var now = Instant.now();
        var yesterday = now.minus(1, ChronoUnit.DAYS).toEpochMilli();
        var tomorrow = now.plus(1, ChronoUnit.DAYS).toEpochMilli();
        return redisTemplate.opsForZSet().reverseRangeByScore(key, yesterday, tomorrow);
    }

    public void removeStoryIdFromUserStories(Long userId, Long storyId) {
        log.debug("Removing story with id {} from user with id {}", storyId, userId);
        var key = constructStoriesKey(userId);
        redisTemplate.opsForZSet().remove(key, storyId);
    }

    private Set<ZSetOperations.TypedTuple<Object>> getStoriesIdsTuples(List<Long> ids) {
        return ids.stream()
                .map(id -> new DefaultTypedTuple<>((Object) id, (double) Instant.now().toEpochMilli()))
                .collect(Collectors.toSet());
    }

    private String constructStoriesKey(Long userId) {
        return USER_PREFIX + userId + STORIES_SUFFIX;
    }

    public void cacheStory(Story story) {
        var storyDto = storyMapper.toStoryDto(story);
        cacheStoryDto(storyDto);
        cacheStoryIdToUserStories(story.getUser().getId(), story);
    }
    
    public void deleteStoryFromCache(Story story) {
        log.debug("Deleting all occurrences of story with id: {}", story.getId());
        var storyId = story.getId();
        var userId = story.getUser().getId();
        removeStoryDtoById(storyId);
        removeStoryIdFromUserStories(userId, storyId);
    }
}
