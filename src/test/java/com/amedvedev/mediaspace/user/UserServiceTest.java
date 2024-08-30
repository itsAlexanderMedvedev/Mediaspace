package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    MockitoSession mockitoSession;

    @BeforeEach
    public void setUp() {
        mockitoSession = Mockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.STRICT_STUBS)
                .startMocking();
    }

    @AfterEach
    public void tearDown() {
        mockitoSession.finishMocking();
    }

    @Test
    public void updateUserSuccessWhenUserExistsAndUsernameValidAndAvailable() {
        // Given
        String oldUsername = "oldUser";
        String newUsername = "newUser";
        UpdateUserRequest updateUserRequest = new UpdateUserRequest(newUsername, "password");
        User existingUser = new User();
        existingUser.setUsername(oldUsername);

        when(userRepository.findByUsernameIgnoreCase(oldUsername)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsernameIgnoreCase(newUsername)).thenReturn(Optional.empty());

        // When
        userService.updateUser(oldUsername, updateUserRequest);

        // Then
        verify(userRepository).save(argThat(user -> user.getUsername().equals(newUsername)));
    }

    @Test
    public void updateUserNewPasswordEncoded() {
        // Given
        String username = "user";
        String newPassword = "newPassword";
        UpdateUserRequest updateUserRequest = new UpdateUserRequest(null, newPassword);
        User existingUser = new User();
        existingUser.setUsername(username);

        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");
        when(userRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(existingUser));

        // When
        userService.updateUser(username, updateUserRequest);

        // Then
        verify(userRepository).save(argThat(user -> user.getPassword().equals("encodedPassword")));
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    public void updateUserNewUsernameSameAsOldThrowsException() {
        // Given
        String username = "user";
        UpdateUserRequest updateUserRequest = new UpdateUserRequest(username, "password");
        User existingUser = new User();
        existingUser.setUsername(username);

        when(userRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(username, updateUserRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("New username is the same as the old one");

        verify(userRepository, never()).save(any());
    }

    @Test
    public void updateNewUsernameAlreadyTakenThrowsException() {
        // Given
        String oldUsername = "oldUsername";
        String newUsername = "newTakenUsername";
        UpdateUserRequest updateUserRequest = new UpdateUserRequest(newUsername, "password");
        User existingUser = new User();
        existingUser.setUsername(oldUsername);

        User userWithTakenUsername = User.builder().username(newUsername).build();

        when(userRepository.findByUsernameIgnoreCase(oldUsername)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsernameIgnoreCase(newUsername)).thenReturn(Optional.of(userWithTakenUsername));


        // When & Then
        assertThatThrownBy(() -> userService.updateUser(oldUsername, updateUserRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    public void updateUserWhenUserDoesNotExistThrowsException() {
        // Given
        String username = "nonExistentUser";
        UpdateUserRequest updateUserRequest = new UpdateUserRequest("newUsername", "password");

        when(userRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(username, updateUserRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");

        verify(userRepository, never()).save(any());
    }
}
