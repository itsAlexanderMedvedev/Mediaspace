package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.user.dto.*;
import com.amedvedev.mediaspace.user.exception.FollowException;
import com.amedvedev.mediaspace.user.exception.UserIsNotDeletedException;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.exception.UserUpdateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserRedisService userRedisService;
    private final PasswordEncoder passwordEncoder;

    public User getCurrentUser() {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Attempting to retrieve user from token directly from database with username: {}", username);

        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));
    }

    public UserDto getCurrentUserDto() {
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.debug("Attempting to current user from token with username: {}", username);
        return getUserDtoFromCacheOrDb(username);
    }

    public UserDto getUserDtoByUsername(String username) {
        return getUserDtoFromCacheOrDb(username);
    }

    public User findUserByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional
    public void followUser(String username) {
        var follower = getCurrentUser();
        var followee = findUserByUsername(username);

        log.debug("User {} is attempting to follow user {}", follower.getUsername(), followee.getUsername());

        verifyUserIsNotTryingToFollowingThemself(follower, followee);
        verifyUserIsNotAlreadyFollowed(follower, followee);

        follower.follow(followee);

        userRepository.save(follower);
        userRedisService.cacheUser(follower);
        userRedisService.cacheUser(followee);

        log.info("User {} successfully followed user {}", follower.getUsername(), followee.getUsername());
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

        log.debug("User {} is attempting to unfollow user {}", follower.getUsername(), followee.getUsername());

        verifyUserIsNotTryingToUnfollowThemself(follower, followee);
        verifyUserIsFollowed(follower, followee);

        follower.unfollow(followee);

        userRepository.save(follower);
        userRedisService.cacheUser(follower);
        userRedisService.cacheUser(followee);

        log.info("User {} successfully unfollowed user {}", follower.getUsername(), followee.getUsername());
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

    @Transactional
    public UpdateUserResponse changeUsername(ChangeUsernameRequest changeUsernameRequest) {
        var user = getCurrentUser();
        var newUsername = changeUsernameRequest.getUsername();

        log.debug("User {} is attempting to change username to {}", user.getUsername(), newUsername);

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

        log.debug("User {} is attempting to change password", user.getUsername());

        verifyPasswordIsCorrect(changePasswordRequest.getOldPassword(), user);
        user.setPassword(passwordEncoder.encode(changePasswordRequest.getPassword()));
        userRepository.save(user);

        log.info("Password successfully change for user with username: {}", user.getUsername());

        return UpdateUserResponse.builder()
                .message("Password changed successfully, please log in again with new credentials")
                .build();
    }

    public void save(User user) {
        var savedUser = userRepository.save(user);
        log.debug("User saved with username: {}", savedUser.getUsername());
        userRedisService.cacheUser(user);
        userRedisService.cacheUsernameToId(savedUser.getUsername(), savedUser.getId());
        log.debug("User cached with username: {}", savedUser.getUsername());
    }

    @Transactional
    public void deleteUser() {
        var user = getCurrentUser();

        log.debug("Deleting user with username: {}", user.getUsername());

        user.setDeleted(true);
        userRepository.save(user);
        clearCachedUserInfo(user);

        log.debug("User deleted with username: {}", user.getUsername());
    }

    private void clearCachedUserInfo(User user) {
        userRedisService.clearCachedUserById(user.getId());
        userRedisService.clearCachedUserIdByUsername(user.getUsername());
    }

    @Transactional
    public RestoreUserResponse restoreUser(RestoreUserRequest request) {
        var user = userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        log.debug("Trying to restore user with username: {}", user.getUsername());

        verifyUserNotDeleted(user);
        verifyPasswordIsCorrect(request.getPassword(), user);

        user.setDeleted(false);
        userRepository.save(user);
        cacheUserAndIdMapping(user);

        log.info("User with username: {} restored successfully", user.getUsername());
        return RestoreUserResponse.builder()
                .message("User restored successfully. Please login to continue.")
                .build();
    }

    private void cacheUserAndIdMapping(User user) {
        userRedisService.cacheUser(user);
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

    private boolean isUsernameFree(String username) {
        return findByUsernameIgnoreCaseAndIncludeSoftDeleted(username).isEmpty();
    }

    public Optional<User> findByUsernameIgnoreCaseAndIncludeSoftDeleted(String username) {
        return userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(username);
    }

    private UserDto getUserDtoFromCacheOrDb(String username) {
        return userRedisService.getCachedUserIdByUsername(username)
            .flatMap(userRedisService::getCachedUserById)
            .orElseGet(() -> getAndCacheUserDtoByUsername(username));
    }

    private UserDto getAndCacheUserDtoByUsername(String username) {
        var user = findUserByUsername(username);
        return cacheAndReturnUserDto(user);
    }

    private UserDto cacheAndReturnUserDto(User user) {
        userRedisService.cacheUser(user);
        log.debug("Cached and retrieved user with username: {}", user.getUsername());
        return userMapper.toUserDto(user);
    }

}
