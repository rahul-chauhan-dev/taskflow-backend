package com.example.taskflow.security;

import com.example.taskflow.entity.Role;
import com.example.taskflow.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    private static User user(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setEmail("ann@example.com");
        u.setRole(role);
        return u;
    }

    @Test
    void aGeneratedTokenParsesBackToTheSameUser() {
        JwtService jwt = new JwtService(SECRET, 60);

        Optional<AuthUser> parsed = jwt.parse(jwt.generate(user(7, Role.ADMIN)));

        assertThat(parsed).isPresent();
        assertThat(parsed.get().id()).isEqualTo(7L);
        assertThat(parsed.get().email()).isEqualTo("ann@example.com");
        assertThat(parsed.get().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRejected() {
        JwtService mine = new JwtService(SECRET, 60);
        JwtService other = new JwtService("another-secret-another-secret-another-1", 60);

        assertThat(mine.parse(other.generate(user(1, Role.USER)))).isEmpty();
    }

    @Test
    void anExpiredTokenIsRejected() {
        JwtService expired = new JwtService(SECRET, -1); // expires one minute in the past

        assertThat(expired.parse(expired.generate(user(1, Role.USER)))).isEmpty();
    }

    @Test
    void tamperedAndGarbageTokensAreRejected() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.generate(user(1, Role.USER));
        String tampered = token.substring(0, token.length() - 5) + "xxxxx";

        assertThat(jwt.parse(tampered)).isEmpty();
        assertThat(jwt.parse("garbage")).isEmpty();
        assertThat(jwt.parse("")).isEmpty();
    }

    @Test
    void aShortSecretIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtService("too-short", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}