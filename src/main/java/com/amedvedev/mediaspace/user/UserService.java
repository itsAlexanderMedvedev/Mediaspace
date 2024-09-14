package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.redis.RedisService;
import com.amedvedev.mediaspace.user.dto.RestoreUserRequest;
import com.amedvedev.mediaspace.user.dto.UpdateUserRequest;
import com.amedvedev.mediaspace.user.dto.UpdateUserResponse;
import com.amedvedev.mediaspace.user.dto.ViewUserResponse;
import com.amedvedev.mediaspace.user.exception.UserIsNotDeletedException;
import com.amedvedev.mediaspace.user.exception.FollowException;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.exception.UserUpdateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final RedisService redisService;

    public User findByUsernameIgnoreCase(String username) {
        return redisService.getCachedUser(username)
                .orElseGet(() -> {
                    log.debug("Retrieving user by username from: {}", username);

                    User user = userRepository.findByUsernameIgnoreCase(username)
                            .orElseThrow(() -> new UserNotFoundException("User not found"));

                    redisService.cacheUser(user);

                    return user;
                });
}

    public void followUser(String username) {
        var follower = getUserFromToken();
        var followee = getUserByUsername(username);

        if (follower.equals(followee)) {
            throw new FollowException("Cannot follow yourself");
        }
        if (follower.getFollowing().contains(followee)) {
            throw new FollowException("User is already followed");
        }

        follower.getFollowing().add(followee);
        followee.getFollowers().add(follower);

        userRepository.save(follower);

        redisService.clearCachedUser(followee.getId());
        redisService.clearCachedUser(follower.getId());
    }

    public void unfollowUser(String username) {
        var follower = getUserFromToken();
        var followee = getUserByUsername(username);

        if (follower.equals(followee)) {
            throw new FollowException("Cannot unfollow yourself");
        }
        if (!follower.getFollowing().contains(followee)) {
            throw new FollowException("Cannot unfollow user that is not followed");
        }

        follower.getFollowing().remove(followee);
        followee.getFollowers().remove(follower);

        userRepository.save(follower);

        redisService.clearCachedUser(followee.getId());
        redisService.clearCachedUser(follower.getId());
    }

    public UpdateUserResponse updateUser(UpdateUserRequest updateUserRequest) {
        var user = getUserFromToken();

        var updateUserResponse = new UpdateUserResponse();
        String newUsername = updateUserRequest.getUsername();

        if (newUsername != null) {
            if (newUsername.equals(user.getUsername())) {
                throw new UserUpdateException("New username is the same as the old one");
            }
            if (!isUsernameFree(newUsername)) {
                throw new UserUpdateException("Username is already taken");
            }
            user.setUsername(newUsername);
            updateUserResponse.setUsername(newUsername);
        }

        if (updateUserRequest.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(updateUserRequest.getPassword()));
            updateUserResponse.setUsername(user.getUsername());
        }

        userRepository.save(user);
        updateUserResponse.setMessage("User updated successfully, please log in again with new credentials");

        if (user.getUsername().equals(newUsername)) {
            redisService.clearCachedUser(user.getId());
        }

        return updateUserResponse;
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public ViewUserResponse me() {
        var user = getUserFromToken();
        return userMapper.toViewUserDto(user);
    }

    public void deleteUser() {
        var user = getUserFromToken();
        user.setDeleted(true);
        userRepository.save(user);
        redisService.clearCachedUser(user.getId());
    }

    public void restoreUser(RestoreUserRequest request) {
        var user = getUserByUsername(request.getUsername());

        if (!user.isDeleted()) {
            throw new UserIsNotDeletedException("User is not deleted");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Bad credentials");
        }

        user.setDeleted(false);
        userRepository.save(user);
    }

    private boolean isUsernameFree(String username) {
        return findByUsernameIgnoreCaseAndIncludeSoftDeleted(username).isEmpty();
    }

    public Optional<User> findByUsernameIgnoreCaseAndIncludeSoftDeleted(String username) {
        return userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(username);
    }

    private User getUserByUsername(String username) {
        return redisService.getCachedUser(username)
                .orElseGet(() -> {
                    User user = userRepository.findByUsernameIgnoreCase(username)
                        .orElseThrow(() -> new UserNotFoundException("User not found"));

                    redisService.cacheUser(user);
                    return user;
                });
    }

    private User getUserFromToken() {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();

        return redisService.getCachedUser(username)
                .orElseGet(() -> {
                    User user = userRepository.findByUsernameIgnoreCase(username)
                        .orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

                    redisService.cacheUser(user);
                    return user;
                });
    }
}
