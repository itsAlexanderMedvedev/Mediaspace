package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.redis.TtlUpdateEntry;
import com.amedvedev.mediaspace.user.dto.UserDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String USER_KEY_PREFIX = "users:";
    private static final String USERNAME_TO_ID_KEY_PREFIX = "username_to_id:";
    private static final String FOLLOWERS_COUNT_KEY_PREFIX = "followers_count:";
    private static final String FOLLOWING_COUNT_KEY_PREFIX = "following_count:";

    private static final int DEFAULT_USER_TTL = 1;
    private static final int DEFAULT_USERNAME_TO_ID_TTL = 24;

    public void cacheUser(User user) {
        log.debug("Caching user with id: {}", user.getId());
        var userDto = userMapper.toUserDto(user);
        tryCacheUserDto(userDto);
    }

    private void tryCacheUserDto(UserDto userDto) {
        String key = USER_KEY_PREFIX + userDto.getId();
        try {
            String userJson = objectMapper.writeValueAsString(userDto);
            redisTemplate.opsForValue().set(key, userJson, DEFAULT_USER_TTL, TimeUnit.HOURS);
            log.debug("Cached user with id: {}", userDto.getId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing user with id: {}", userDto.getId(), e);
        }
    }

    public Optional<UserDto> getCachedUserById(Long id) {
        log.debug("Retrieving user from cache with id: {}", id);

        String key = USER_KEY_PREFIX + id;
        var userDto = extractUserDtoWithKey(key);
        userDto.ifPresent(dto -> refreshKeyTtl(new TtlUpdateEntry(key, DEFAULT_USER_TTL, TimeUnit.HOURS)));

        return userDto;
    }

    private void refreshKeyTtl(TtlUpdateEntry ttlUpdateEntry) {
        String key = ttlUpdateEntry.key();
        redisTemplate.expire(key, ttlUpdateEntry.ttl(), ttlUpdateEntry.timeUnit());
        log.debug("TTL updated for key: {}", key);
    }

    private Optional<UserDto> extractUserDtoWithKey(String key) {
        String userJson = redisTemplate.opsForValue().get(key);

        if (userJson != null) {
            return deserializeUserDto(key, userJson);
        }

        log.debug("User not found in cache with key: {}", key);
        return Optional.empty();
    }

    private Optional<UserDto> deserializeUserDto(String key, String userJson) {
        try {
            return Optional.of(objectMapper.readValue(userJson, UserDto.class));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing user from cache with key: {}", key, e);
            return Optional.empty();
        }
    }

    public void clearCachedUserById(Long id) {
        String key = USER_KEY_PREFIX + id;
        redisTemplate.delete(key);
        log.debug("Cleared cached user data for user with id: {}", id);
    }

    public void cacheUsernameToId(String username, Long id) {
        String key = USERNAME_TO_ID_KEY_PREFIX + username;
        redisTemplate.opsForValue().set(key, id.toString(), DEFAULT_USERNAME_TO_ID_TTL, TimeUnit.HOURS);
        log.debug("Cached username-to-id mapping for username: {}", username);
    }

    public Optional<Long> getCachedUserIdByUsername(String username) {
        var key = USERNAME_TO_ID_KEY_PREFIX + username;
        var userIdStr = redisTemplate.opsForValue().get(key);

        if (userIdStr == null) {
            log.debug("User ID not found in cache for username: {}", username);
            return Optional.empty();
        }

        log.debug("Retrieved user ID from cache for username: {}", username);
        refreshKeyTtl(new TtlUpdateEntry(key, DEFAULT_USERNAME_TO_ID_TTL, TimeUnit.HOURS));

        return parseUserId(username, userIdStr);
    }

    private Optional<Long> parseUserId(String username, String userIdStr) {
        try {
            return Optional.of(Long.parseLong(userIdStr));
        } catch (NumberFormatException e) {
            log.error("Error parsing cached user ID for username: {}. Invalid number format.", username, e);
            return Optional.empty();
        }
    }

    public void clearCachedUserIdByUsername(String username) {
        String key = USERNAME_TO_ID_KEY_PREFIX + username;
        redisTemplate.delete(key);
        log.debug("Cleared cached user ID for username: {}", username);
    }
}
