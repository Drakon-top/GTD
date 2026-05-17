package com.gtd.backend.auth.repository;

import com.gtd.backend.auth.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveUser_whenValidData() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .build();

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$hashedpassword");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByEmail_whenUserExists() {
        User user = User.builder()
                .email("find@example.com")
                .passwordHash("$2a$10$hash")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("find@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find@example.com");
    }

    @Test
    void shouldReturnEmpty_whenEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnTrue_whenEmailExists() {
        User user = User.builder()
                .email("exists@example.com")
                .passwordHash("$2a$10$hash")
                .build();
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
    }

    @Test
    void shouldReturnFalse_whenEmailDoesNotExist() {
        assertThat(userRepository.existsByEmail("nope@example.com")).isFalse();
    }

    @Test
    void shouldRejectDuplicateEmail() {
        User user1 = User.builder()
                .email("dup@example.com")
                .passwordHash("$2a$10$hash1")
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .email("dup@example.com")
                .passwordHash("$2a$10$hash2")
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void shouldSetTimestamps_whenPersisting() {
        User user = User.builder()
                .email("time@example.com")
                .passwordHash("$2a$10$hash")
                .build();

        User saved = userRepository.saveAndFlush(user);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }
}
