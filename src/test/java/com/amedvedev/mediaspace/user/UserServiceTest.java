package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.user.dto.ChangePasswordRequest;
import com.amedvedev.mediaspace.user.dto.UserDto;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.dto.ChangeUsernameRequest;
import com.amedvedev.mediaspace.user.exception.UserUpdateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Mock
    private SecurityContextHolder securityContextHolder;

    @Mock
    private UserRedisService userRedisService;

    @Mock
    private UserMapper userMapper;

    private MockitoSession mockitoSession;

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
    public void changeUsernameSuccessWhenUserExistsAndUsernameValidAndAvailable() {
        // Given
        String oldUsername = "oldUser";
        String newUsername = "newUser";

        ChangeUsernameRequest changeUsernameRequest = new ChangeUsernameRequest(newUsername);

        User existingUser = new User();
        existingUser.setUsername(oldUsername);

        var authentication = mock(UsernamePasswordAuthenticationToken.class);
        var securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("oldUser");
        when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(newUsername)).thenReturn(Optional.empty());

        // When
        userService.changeUsername(changeUsernameRequest);

        // Then
        verify(userRepository).save(argThat(user -> user.getUsername().equals(newUsername)));
    }

    @Test
    public void changePasswordNewPasswordEncoded() {
        // Given
        String username = "user";
        String newPassword = "newPassword";
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest("oldPassword", newPassword);
        User existingUser = new User();
        existingUser.setUsername(username);

        var authentication = mock(UsernamePasswordAuthenticationToken.class);
        var securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user");

        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");
        when(userRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(existingUser));

        // When
        userService.changePassword(changePasswordRequest);

        // Then
        verify(userRepository).save(argThat(user -> user.getPassword().equals("encodedPassword")));
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    public void changeUsernameNewUsernameSameAsOldThrowsException() {
        // Given
        String username = "user";
        ChangeUsernameRequest changeUsernameRequest = new ChangeUsernameRequest(username);
        User existingUser = new User();
        existingUser.setUsername(username);

        var authentication = mock(UsernamePasswordAuthenticationToken.class);
        var securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user");
        when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));

        // When & Then
        assertThatThrownBy(() -> userService.changeUsername(changeUsernameRequest))
                .isInstanceOf(UserUpdateException.class)
                .hasMessage("New username is the same as the old one");

        verify(userRepository, never()).save(any());
    }

    @Test
    public void changeUsernameNewUsernameAlreadyTakenThrowsException() {
        // Given
        String oldUsername = "oldUsername";
        String newUsername = "newTakenUsername";
        ChangeUsernameRequest changeUsernameRequest = new ChangeUsernameRequest(newUsername);

        User existingUser = new User();
        existingUser.setUsername(oldUsername);

        User userWithTakenUsername = User.builder().username(newUsername).build();

        var authentication = mock(UsernamePasswordAuthenticationToken.class);
        var securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("oldUsername");

        when(userRepository.findByUsernameIgnoreCase("oldUsername")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(newUsername)).thenReturn(Optional.of(userWithTakenUsername));


        // When & Then
        assertThatThrownBy(() -> userService.changeUsername(changeUsernameRequest))
                .isInstanceOf(UserUpdateException.class)
                .hasMessage("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    public void changeUsernameWhenUserDoesNotExistThrowsException() {
        // Given
        ChangeUsernameRequest changeUsernameRequest = new ChangeUsernameRequest("newUsername");

        var authentication = mock(UsernamePasswordAuthenticationToken.class);
        var securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("");


        when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.changeUsername(changeUsernameRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Authentication object is invalid or does not contain a username");

        verify(userRepository, never()).save(any());
    }
}
