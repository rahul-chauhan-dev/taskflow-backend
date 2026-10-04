package com.example.taskflow.service;

import com.example.taskflow.dto.AuthResponse;
import com.example.taskflow.dto.LoginRequest;
import com.example.taskflow.dto.RegisterRequest;
import com.example.taskflow.entity.Role;
import com.example.taskflow.entity.User;
import com.example.taskflow.exception.UnauthorizedException;
import com.example.taskflow.repository.UserRepository;
import com.example.taskflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        // the constructor hashes a dummy password, used to equalize login timing
        when(passwordEncoder.encode("not-a-real-password")).thenReturn("dummy-hash");
        service = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    private static User userWithHash(String hash) {
        User u = new User();
        u.setId(1L);
        u.setName("Ann");
        u.setEmail("ann@example.com");
        u.setPasswordHash(hash);
        return u;
    }

    @Test
    void anUnknownEmailStillRunsAPasswordCheck() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("whatever", "dummy-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest(" Nobody@Example.com ", "whatever")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        // the BCrypt comparison ran anyway, so response time does not reveal unknown emails
        verify(passwordEncoder).matches("whatever", "dummy-hash");
    }

    @Test
    void aWrongPasswordGivesTheSameMessageAndNoToken() {
        when(userRepository.findByEmail("ann@example.com"))
                .thenReturn(Optional.of(userWithHash("real-hash")));
        when(passwordEncoder.matches("wrong", "real-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("ann@example.com", "wrong")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid email or password");

        verify(jwtService, never()).generate(any());
    }

    @Test
    void theCorrectPasswordReturnsATokenAndTheUser() {
        User ann = userWithHash("real-hash");
        when(userRepository.findByEmail("ann@example.com")).thenReturn(Optional.of(ann));
        when(passwordEncoder.matches("password123", "real-hash")).thenReturn(true);
        when(jwtService.generate(ann)).thenReturn("jwt-token");

        AuthResponse result = service.login(new LoginRequest("ann@example.com", "password123"));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.user().email()).isEqualTo("ann@example.com");
    }

    @Test
    void registerNormalisesTheEmailHashesThePasswordAndForcesTheUserRole() {
        when(userRepository.existsByEmail("ann@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generate(any(User.class))).thenReturn("jwt-token");

        service.register(new RegisterRequest("  Ann  ", " Ann@Example.COM ", "password123"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User stored = saved.getValue();
        assertThat(stored.getEmail()).isEqualTo("ann@example.com");
        assertThat(stored.getName()).isEqualTo("Ann");
        assertThat(stored.getPasswordHash()).isEqualTo("hashed").isNotEqualTo("password123");
        assertThat(stored.getRole()).isEqualTo(Role.USER);
    }
}