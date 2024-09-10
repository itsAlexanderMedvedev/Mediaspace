package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.user.dto.RestoreUserRequest;
import com.amedvedev.mediaspace.user.dto.UpdateUserRequest;
import com.amedvedev.mediaspace.user.dto.UpdateUserResponse;
import com.amedvedev.mediaspace.user.dto.ViewUserResponse;
import com.amedvedev.mediaspace.user.exception.UserIsNotDeletedException;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.exception.UserUpdateException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UpdateUserResponse updateUser(UpdateUserRequest updateUserRequest) {
        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

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

        return updateUserResponse;
    }

    private boolean isUsernameFree(String username) {
        return findByUsernameIgnoreCaseAndIncludeSoftDeleted(username).isEmpty();
    }

    public Optional<User> findByUsernameIgnoreCase(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    public Optional<User> findByUsernameIgnoreCaseAndIncludeSoftDeleted(String username) {
        return userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(username);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public ViewUserResponse me() {
        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

        return userMapper.toViewUserDto(user);
    }

    public void deleteUser() {
        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

        user.setDeleted(true);
        userRepository.save(user);
    }

    public void restoreUser(RestoreUserRequest request) {
        var user = userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.isDeleted()) {
            user.setDeleted(false);
        } else {
            throw new UserIsNotDeletedException("User is not deleted");
        }

        userRepository.save(user);
    }

}
