package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.dto.UpdateUserRequest;
import com.amedvedev.mediaspace.user.dto.UpdateUserResponse;
import com.amedvedev.mediaspace.user.dto.ViewUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UpdateUserResponse updateUser(String username, UpdateUserRequest updateUserRequest) {
        var userOptional = findByUsernameIgnoreCase(username);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            throw new UserNotFoundException("User not found");
        }

        var updateUserResponse = new UpdateUserResponse();
        String newUsername = updateUserRequest.getUsername();
        if (newUsername != null) {
            if (newUsername.equals(user.getUsername())) {
                throw new IllegalArgumentException("New username is the same as the old one");
            }
            if (!isUsernameFree(newUsername)) {
                throw new IllegalArgumentException("Username is already taken");
            }
            user.setUsername(newUsername);
            updateUserResponse.setUsername(newUsername);
        }
        if (updateUserRequest.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(updateUserRequest.getPassword()));
            updateUserResponse.setUsername(user.getUsername());
        }
        userRepository.save(user);
        updateUserResponse.setMessage("User updated successfully");

        return updateUserResponse;
    }

    private boolean isUsernameFree(String username) {
        return findByUsernameIgnoreCase(username).isEmpty();
    }

    public Optional<User> findByUsernameIgnoreCase(String username) {
        return userRepository.findByUsernameIgnoreCase(username);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public ViewUserResponse me() {
        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UsernameNotFoundException("Authentication object is invalid or does not contain a username"));

        return userMapper.toViewUserDto(user);
    }

    public void deleteUser() {
        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UsernameNotFoundException("Authentication object is invalid or does not contain a username"));

        user.setDeleted(true);
        userRepository.save(user);
    }
}
