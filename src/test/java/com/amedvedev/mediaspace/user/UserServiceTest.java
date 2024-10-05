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

    private void mockAuthentication(String oldUser) {
        var authentication = mock(UsernamePasswordAuthenticationToken.class);
        var securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(oldUser);
    }

    @Test
    public void changeUsernameSuccessWhenUserExistsAndUsernameValidAndAvailable() {
        var oldUsername = "oldUser";
        var newUsername = "newUser";
        var changeUsernameRequest = new ChangeUsernameRequest(newUsername);
        var existingUser = User.builder().username(oldUsername).build();

        mockAuthentication(oldUsername);
        when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(newUsername)).thenReturn(Optional.empty());


        userService.changeUsername(changeUsernameRequest);


        verify(userRepository).save(argThat(user -> user.getUsername().equals(newUsername)));
    }

    @Test
    public void changePasswordNewPasswordEncoded() {
        var username = "user";
        var newPassword = "newPassword";
        var changePasswordRequest = new ChangePasswordRequest("oldPassword", newPassword);
        var existingUser = User.builder().username(username).build();

        mockAuthentication(username);

        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedPassword");
        when(userRepository.findByUsernameIgnoreCase(username)).thenReturn(Optional.of(existingUser));


        userService.changePassword(changePasswordRequest);


        verify(userRepository).save(argThat(user -> user.getPassword().equals("encodedPassword")));
        verify(passwordEncoder).encode(newPassword);
    }

    @Test
    public void changeUsernameNewUsernameSameAsOldThrowsException() {
        var username = "user";
        var changeUsernameRequest = new ChangeUsernameRequest(username);
        var existingUser = User.builder().username(username).build();

        mockAuthentication(username);
        when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.changeUsername(changeUsernameRequest))
                .isInstanceOf(UserUpdateException.class)
                .hasMessage("New username is the same as the old one");

        verify(userRepository, never()).save(any());
    }

    @Test
    public void changeUsernameNewUsernameAlreadyTakenThrowsException() {
        var oldUsername = "oldUsername";
        var newUsername = "newTakenUsername";
        var changeUsernameRequest = new ChangeUsernameRequest(newUsername);
        var existingUser = User.builder().username(oldUsername).build();
        var userWithTakenUsername = User.builder().username(newUsername).build();

        mockAuthentication("oldUsername");

        when(userRepository.findByUsernameIgnoreCase("oldUsername")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsernameIgnoreCaseAndIncludeSoftDeleted(newUsername)).thenReturn(Optional.of(userWithTakenUsername));


        assertThatThrownBy(() -> userService.changeUsername(changeUsernameRequest))
                .isInstanceOf(UserUpdateException.class)
                .hasMessage("Username is already taken");


        verify(userRepository, never()).save(any());
    }
}
