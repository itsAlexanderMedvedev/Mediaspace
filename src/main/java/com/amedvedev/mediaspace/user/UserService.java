package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.dto.UpdateUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void updateUser(String username, UpdateUserDto updateUserDto) {
        var userOptional = findByUsernameIgnoreCase(username);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            throw new UserNotFoundException("User not found");
        }

        String newUsername = updateUserDto.getUsername();
        if (newUsername != null) {
            if (newUsername.equals(user.getUsername())) {
                throw new IllegalArgumentException("New username is the same as the old one");
            }
            if (!isUsernameFree(newUsername)) {
                throw new IllegalArgumentException("Username is already taken");
            }
            user.setUsername(newUsername);
        }
        if (updateUserDto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(updateUserDto.getPassword()));
        }
        userRepository.save(user);
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
}
