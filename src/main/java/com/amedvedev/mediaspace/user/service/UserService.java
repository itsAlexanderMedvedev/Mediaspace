package com.amedvedev.mediaspace.user.service;

import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserMapper;
import com.amedvedev.mediaspace.user.UserRepository;
import com.amedvedev.mediaspace.user.dto.*;
import com.amedvedev.mediaspace.user.exception.FollowException;
import com.amedvedev.mediaspace.user.exception.UserIsNotDeletedException;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.exception.UserUpdateException;
import com.amedvedev.mediaspace.user.follow.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserRedisService userRedisService;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Retrieving user from token with username: {}", username);

        return userRepository.findByUsernameIgnoreCase(username).orElseThrow(
                () -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));
    }

    public UserDto getCurrentUserDto() {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Getting current user from token with username: {}", username);
        return getUserDto(username);
    }

    public UserDto getUserDtoByUsername(String username) {
        log.debug("Getting user dto by username: {}", username);
        return getUserDto(username);
    }

    public User findUserByUsername(String username) {
        log.debug("Fetching user by username from database with username: {}", username);
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public void followUser(String username) {
        var follower = getCurrentUser();
        var followee = findUserByUsername(username);

        log.debug("User {} is following user {}", follower.getUsername(), followee.getUsername());

        verifyUserIsNotTryingToFollowingThemself(follower, followee);
        verifyUserIsNotAlreadyFollowed(follower, followee);

        follower.follow(followee);

        userRepository.save(follower);
        userRedisService.cacheUserDto(userMapper.toUserDto(follower));
        userRedisService.cacheUserDto(userMapper.toUserDto(followee));
    }

    private void verifyUserIsNotAlreadyFollowed(User follower, User followee) {
        if (follower.getFollowing().contains(followee)) {
            log.warn("User {} is already following user {}", follower.getUsername(), followee.getUsername());
            throw new FollowException("User is already followed");
        }
    }

    private void verifyUserIsNotTryingToFollowingThemself(User follower, User followee) {
        if (follower.equals(followee)) {
            log.warn("User {} attempted to follow themselves", follower.getUsername());
            throw new FollowException("Cannot follow yourself");
        }
    }

    @Transactional
    public void unfollowUser(String username) {
        var follower = getCurrentUser();
        var followee = findUserByUsername(username);

        log.debug("User {} is unfollowing user {}", follower.getUsername(), followee.getUsername());

        verifyUserIsNotTryingToUnfollowThemself(follower, followee);
        verifyUserIsFollowed(follower, followee);

        follower.unfollow(followee);

        userRepository.save(follower);
        userRedisService.cacheUserDto(userMapper.toUserDto(follower));
        userRedisService.cacheUserDto(userMapper.toUserDto(followee));
    }

    private void verifyUserIsFollowed(User follower, User followee) {
        if (!follower.getFollowing().contains(followee)) {
            log.warn("User {} is not following user {}", follower.getUsername(), followee.getUsername());
            throw new FollowException("Cannot unfollow user that is not followed");
        }
    }

    private void verifyUserIsNotTryingToUnfollowThemself(User follower, User followee) {
        if (follower.equals(followee)) {
            log.warn("User {} attempted to unfollow themselves", follower.getUsername());
            throw new FollowException("Cannot unfollow yourself");
        }
    }

    public int getFollowersCount(Long id) {
        log.debug("Getting followers count for user with id: {}", id);
        return userRedisService.getFollowersCount(id).orElseGet(() -> followRepository.countFollowersByUserId(id));
    }

    public int getFollowingCount(Long id) {
        log.debug("Getting following count for user with id: {}", id);
        return userRedisService.getFollowingCount(id).orElseGet(() -> followRepository.countFollowingByUserId(id));
    }

    @Transactional(readOnly = true)
    public List<Long> getFollowersIdsByUserId(Long id) {
        log.debug("Getting followers of user with id: {}", id);
        return followRepository.findFollowersIdsByUserId(id);
    }

    @Transactional
    public UpdateUserResponse changeUsername(ChangeUsernameRequest changeUsernameRequest) {
        var user = getCurrentUser();
        var newUsername = changeUsernameRequest.getUsername();
        log.info("User {} is attempting to change username to {}", user.getUsername(), newUsername);

        verifyNewUsernameIsDifferent(newUsername, user);
        verifyNewUsernameIsFree(newUsername, user);

        user.setUsername(newUsername);
        userRepository.save(user);
        cacheUserAndIdMapping(user);

        log.info("User {} successfully changed username to {}", user.getUsername(), newUsername);

        return UpdateUserResponse.builder()
                .message("Username changed successfully, please log in again with new credentials")
                .build();
    }

    private void verifyNewUsernameIsFree(String newUsername, User user) {
        if (!isUsernameFree(newUsername)) {
            log.warn("User {} attempted to change username to an existing one", user.getUsername());
            throw new UserUpdateException("Username is already taken");
        }
    }

    private void verifyNewUsernameIsDifferent(String newUsername, User user) {
        if (newUsername.equals(user.getUsername())) {
            log.warn("User {} attempted to change username to the same one", user.getUsername());
            throw new UserUpdateException("New username is the same as the old one");
        }
    }

    @Transactional
    public UpdateUserResponse changePassword(ChangePasswordRequest changePasswordRequest) {
        var user = getCurrentUser();

        log.debug("User {} is changing change password", user.getUsername());

        verifyPasswordIsCorrect(changePasswordRequest.getOldPassword(), user);
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getPassword()));
        userRepository.save(user);

        log.info("Password successfully change for user with username: {}", user.getUsername());

        return UpdateUserResponse.builder()
                .message("Password changed successfully, please log in again with new credentials")
                .build();
    }

    @Transactional
    public void save(User user) {
        log.debug("Saving user with username: {}", user.getUsername());
        var savedUser = userRepository.save(user);
        log.debug("Caching user with username: {}", user.getUsername());
        userRedisService.cacheUserDto(userMapper.toUserDto(savedUser));
        userRedisService.cacheUsernameToId(savedUser.getUsername(), savedUser.getId());
    }

    @Transactional
    public void deleteUser() {
        var user = getCurrentUser();

        log.debug("Deleting user with username: {}", user.getUsername());

        user.setDeleted(true);
        userRepository.save(user);
        clearCachedUserInfo(user);
    }

    private void clearCachedUserInfo(User user) {
        userRedisService.clearCachedUserById(user.getId());
        userRedisService.clearCachedUserIdByUsername(user.getUsername());
    }

    @Transactional
    public RestoreUserResponse restoreUser(RestoreUserRequest request) {
        var user = userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        log.debug("Restoring user with username: {}", user.getUsername());

        verifyUserNotDeleted(user);
        verifyPasswordIsCorrect(request.getPassword(), user);

        user.setDeleted(false);
        userRepository.save(user);
        cacheUserAndIdMapping(user);

        return RestoreUserResponse.builder()
                .message("User restored successfully. Please login to continue.")
                .build();
    }

    private void cacheUserAndIdMapping(User user) {
        userRedisService.cacheUserDto(userMapper.toUserDto(user));
        userRedisService.cacheUsernameToId(user.getUsername(), user.getId());
    }

    private void verifyPasswordIsCorrect(String password, User user) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("User with username: {} attempted to restore with an incorrect password", user.getUsername());
            throw new BadCredentialsException("Bad credentials");
        }
    }

    private void verifyUserNotDeleted(User user) {
        if (!user.isDeleted()) {
            log.warn("User {} is not deleted", user.getUsername());
            throw new UserIsNotDeletedException("User is not deleted");
        }
    }

    public boolean isUsernameFree(String username) {
        return userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(username).isEmpty();
    }

    public User findByUsernameIgnoreCaseAndIncludeSoftDeleted(String username) {
        log.info("Fetching user by username including soft deleted with username: {}", username);
        return userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(username).orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UserNotFoundException("User not found");
                }
        );
    }

    private UserDto getUserDto(String username) {
        return userRedisService.getCachedUserIdByUsername(username)
            .flatMap(userRedisService::getUserDtoById)
            .orElseGet(() -> {
                log.debug("User with username: {} not found in cache", username);
                return getAndCacheUserDtoByUsername(username);
            });
    }

    private UserDto getAndCacheUserDtoByUsername(String username) {
        var user = findUserByUsername(username);
        userRedisService.cacheUserDto(userMapper.toUserDto(user));
        return userMapper.toUserDto(user);
    }
}
