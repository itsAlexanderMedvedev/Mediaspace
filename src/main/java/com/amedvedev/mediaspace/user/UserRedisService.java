package com.amedvedev.mediaspace.user;

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

    private static final String USER_KEY_PREFIX = "user:";
    private static final String USERNAME_TO_ID_KEY_PREFIX = "username_to_id:";
    private static final String FOLLOWERS_COUNT_KEY_SUFFIX = ":followers_count";
    private static final String FOLLOWING_COUNT_KEY_SUFFIX = ":following_count";

    private static final int DEFAULT_USER_TTL = 1;
    private static final int DEFAULT_USERNAME_TO_ID_TTL = 24;

    public void cacheUser(User user) {
        log.debug("Caching user with id: {}", user.getId());
        var userDto = userMapper.toUserDto(user);
        var key = USER_KEY_PREFIX + userDto.getId();
        redisTemplate.opsForValue().set(key, userDto, DEFAULT_USER_TTL, TimeUnit.HOURS);
    }

    public Optional<UserDto> getUserDtoById(Long id) {
        log.debug("Retrieving UserDto from cache with id: {}", id);
        var key = USER_KEY_PREFIX + id;
        UserDto userJson = (UserDto) redisTemplate.opsForValue().get(key);

        if (userJson == null) {
            log.debug("User not found in cache with key: {}", key);
            return Optional.empty();
        }

        refreshKeyTtl(key, DEFAULT_USER_TTL, TimeUnit.HOURS);
        return Optional.of(userJson);
    }

    public void clearCachedUserById(Long id) {
        log.debug("Clearing cached user data for user with id: {}", id);
        var key = USER_KEY_PREFIX + id;
        redisTemplate.delete(key);
    }

    public void cacheUsernameToId(String username, Long id) {
        log.debug("Caching username-to-id mapping for username: {}", username);
        var key = USERNAME_TO_ID_KEY_PREFIX + username;
        redisTemplate.opsForValue().set(key, id.toString(), DEFAULT_USERNAME_TO_ID_TTL, TimeUnit.HOURS);
    }

    public Optional<Long> getCachedUserIdByUsername(String username) {
        log.debug("Retrieving user ID from cache for username: {}", username);
        var key = USERNAME_TO_ID_KEY_PREFIX + username;
        Long userIdStr = (Long) redisTemplate.opsForValue().get(key);

        if (userIdStr == null) {
            log.debug("User ID not found in cache for username: {}", username);
            return Optional.empty();
        }

        refreshKeyTtl(key, DEFAULT_USERNAME_TO_ID_TTL, TimeUnit.HOURS);
        return Optional.of(userIdStr);
    }

    public void clearCachedUserIdByUsername(String username) {
        String key = USERNAME_TO_ID_KEY_PREFIX + username;
        redisTemplate.delete(key);
        log.debug("Cleared cached user ID for username: {}", username);
    }

    public Optional<Integer> getFollowersCount(Long userId) {
        return getCountFromCache(userId, FOLLOWERS_COUNT_KEY_SUFFIX);
    }

    public Optional<Integer> getFollowingCount(Long userId) {
        return getCountFromCache(userId, FOLLOWING_COUNT_KEY_SUFFIX);
    }

    private Optional<Integer> getCountFromCache(Long userId, String countTypePostfix) {
        // If countTypePostfix is ":followers_count", lookingFor will be "followers count"
        var lookingFor = countTypePostfix.replaceAll("([:_])", " ").trim();
        log.debug("Retrieving {} from cache for user with id: {}", lookingFor, userId);
        var key = USER_KEY_PREFIX + userId + countTypePostfix;
        Integer count = (Integer) redisTemplate.opsForValue().get(key);

        if (count == null) {
            log.debug("{} count not found in cache for user with id: {}", lookingFor, userId);
            return Optional.empty();
        }

        refreshKeyTtl(key, DEFAULT_USER_TTL, TimeUnit.HOURS);
        return Optional.of(count);
    }


    private void refreshKeyTtl(String key, int ttl, TimeUnit timeUnit) {
        redisTemplate.expire(key, ttl, timeUnit);
        log.debug("TTL updated for key: {}", key);
    }
}
