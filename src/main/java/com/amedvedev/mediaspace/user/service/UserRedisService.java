package com.amedvedev.mediaspace.user.service;

import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserMapper;
import com.amedvedev.mediaspace.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRedisService {

    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String USER_PREFIX = "user:";
    private static final String USERNAME_TO_ID_PREFIX = "username_to_id:";
    private static final String FOLLOWERS_COUNT_SUFFIX = ":followers_count";
    private static final String FOLLOWING_COUNT_SUFFIX = ":following_count";

    private static final int DEFAULT_USER_TTL = 1;
    private static final int DEFAULT_USERNAME_TO_ID_TTL = 24;

    public void cacheUser(User user) {
        log.debug("Caching id mapping and dto of user with id: {}", user.getId());
        var userDto = userMapper.toUserDto(user);
        cacheUserDto(userDto);
        cacheUsernameToIdMapping(user.getUsername(), user.getId());
    }

    private void cacheUsernameToIdMapping(String username, Long id) {
        log.debug("Caching username-to-id mapping for username: {}", username);
        var key = constructUsernameToIdMappingKey(username);
        redisTemplate.opsForValue().set(key, id, DEFAULT_USERNAME_TO_ID_TTL, TimeUnit.HOURS);
    }

    public void cacheUserDto(UserDto userDto) {
        log.debug("Caching dto of user with id: {}", userDto.getId());
        var key = constructUserKey(userDto.getId());
        redisTemplate.opsForValue().set(key, userDto, DEFAULT_USER_TTL, TimeUnit.HOURS);
    }

    public Optional<UserDto> getUserDtoByUsername(String username) {
        log.debug("Retrieving user by username: {}", username);
        var userId = getCachedUserIdByUsername(username);
        return userId.flatMap(this::getUserDtoById);
    }

    private Optional<Long> getCachedUserIdByUsername(String username) {
        var key = constructUsernameToIdMappingKey(username);
        var userIdObj = redisTemplate.opsForValue().get(key);

        if (userIdObj == null) {
            log.debug("User ID not found in cache for username: {}", username);
            return Optional.empty();
        }

        if (userIdObj instanceof Number) {
            long userId = ((Number) userIdObj).longValue();
            refreshKeyTtl(key, DEFAULT_USERNAME_TO_ID_TTL, TimeUnit.HOURS);
            return Optional.of(userId);
        }

        log.error("Unexpected type for user ID in cache. Expected a Number, but found object of class {}", userIdObj.getClass());
        return Optional.empty();
    }

    private Optional<UserDto> getUserDtoById(Long id) {
        log.debug("Retrieving UserDto from cache with id: {}", id);
        var key = constructUserKey(id);
        var userDto = (UserDto) redisTemplate.opsForValue().get(key);

        if (userDto == null) {
            log.debug("User not found in cache with key: {}", key);
            return Optional.empty();
        }

        refreshKeyTtl(key, DEFAULT_USER_TTL, TimeUnit.HOURS);
        return Optional.of(userDto);
    }

    public void deleteUser(User user) {
        log.debug("Deleting id mapping and dto of user id: {}", user.getId());
        deleteUserDtoById(user.getId());
        clearCachedUserIdByUsername(user.getUsername());
    }

    private void deleteUserDtoById(Long id) {
        log.debug("Clearing cached user data for user with id: {}", id);
        var key = constructUserKey(id);
        redisTemplate.delete(key);
    }
    
    private void clearCachedUserIdByUsername(String username) {
        var key = constructUsernameToIdMappingKey(username);
        redisTemplate.delete(key);
        log.debug("Cleared cached user ID for username: {}", username);
    }

    public Optional<Integer> getFollowersCount(Long userId) {
        return getCountFromCache(userId, FOLLOWERS_COUNT_SUFFIX);
    }

    public Optional<Integer> getFollowingCount(Long userId) {
        return getCountFromCache(userId, FOLLOWING_COUNT_SUFFIX);
    }

    private Optional<Integer> getCountFromCache(Long userId, String countTypePostfix) {
        // If countTypePostfix is ":followers_count", lookingFor will be "followers count"
        var lookingFor = countTypePostfix.replaceAll("([:_])", " ").trim();
        log.debug("Retrieving {} from cache for user with id: {}", lookingFor, userId);
        var key = USER_PREFIX + userId + countTypePostfix;
        Integer count = (Integer) redisTemplate.opsForValue().get(key);

        if (count == null) {
            log.debug("{} not found in cache for user with id: {}", lookingFor, userId);
            return Optional.empty();
        }

        refreshKeyTtl(key, DEFAULT_USER_TTL, TimeUnit.HOURS);
        return Optional.of(count);
    }

    private void refreshKeyTtl(String key, int ttl, TimeUnit timeUnit) {
        log.debug("Updating TTL for key: {}", key);
        redisTemplate.expire(key, ttl, timeUnit);
    }

    private String constructUserKey(Long id) {
        return USER_PREFIX + id;
    }

    private String constructUsernameToIdMappingKey(String username) {
        return USERNAME_TO_ID_PREFIX + username;
    }
}
