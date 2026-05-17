package com.gtd.backend.context.repository;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.model.ContextTheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ContextRepositoryTest {

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        contextRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.saveAndFlush(
                User.builder()
                        .email("ctx-test@example.com")
                        .passwordHash("$2a$10$hash")
                        .build()
        );
    }

    @Test
    void shouldSaveContext_whenValidData() {
        Context context = Context.builder()
                .user(testUser)
                .name("Work")
                .theme(ContextTheme.FORMAL)
                .icon("briefcase")
                .sortOrder(0)
                .build();

        Context saved = contextRepository.saveAndFlush(context);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Work");
        assertThat(saved.getTheme()).isEqualTo(ContextTheme.FORMAL);
        assertThat(saved.getIcon()).isEqualTo("briefcase");
        assertThat(saved.getSortOrder()).isZero();
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindByUserIdAndNotDeleted_whenContextsExist() {
        contextRepository.saveAndFlush(buildContext("Work", ContextTheme.FORMAL, 0, false));
        contextRepository.saveAndFlush(buildContext("Home", ContextTheme.NATURE, 1, false));
        contextRepository.saveAndFlush(buildContext("Deleted", ContextTheme.DARK, 2, true));

        List<Context> found = contextRepository
                .findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(testUser.getId());

        assertThat(found).hasSize(2);
        assertThat(found.get(0).getName()).isEqualTo("Work");
        assertThat(found.get(1).getName()).isEqualTo("Home");
    }

    @Test
    void shouldReturnEmptyList_whenNoActiveContexts() {
        contextRepository.saveAndFlush(buildContext("Deleted", ContextTheme.DARK, 0, true));

        List<Context> found = contextRepository
                .findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(testUser.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindByIdAndNotDeleted_whenContextExists() {
        Context saved = contextRepository.saveAndFlush(
                buildContext("Work", ContextTheme.FORMAL, 0, false));

        Optional<Context> found = contextRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Work");
    }

    @Test
    void shouldReturnEmpty_whenContextIsDeleted() {
        Context saved = contextRepository.saveAndFlush(
                buildContext("Deleted", ContextTheme.DARK, 0, true));

        Optional<Context> found = contextRepository.findByIdAndIsDeletedFalse(saved.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void shouldCountActiveContexts_whenMixed() {
        contextRepository.saveAndFlush(buildContext("Work", ContextTheme.FORMAL, 0, false));
        contextRepository.saveAndFlush(buildContext("Home", ContextTheme.NATURE, 1, false));
        contextRepository.saveAndFlush(buildContext("Deleted", ContextTheme.DARK, 2, true));

        int count = contextRepository.countByUserIdAndIsDeletedFalse(testUser.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldReturnZeroCount_whenNoActiveContexts() {
        int count = contextRepository.countByUserIdAndIsDeletedFalse(testUser.getId());

        assertThat(count).isZero();
    }

    @Test
    void shouldOrderBySortOrder_whenMultipleContexts() {
        contextRepository.saveAndFlush(buildContext("Third", ContextTheme.DARK, 2, false));
        contextRepository.saveAndFlush(buildContext("First", ContextTheme.MINIMALIST, 0, false));
        contextRepository.saveAndFlush(buildContext("Second", ContextTheme.DESIGN, 1, false));

        List<Context> found = contextRepository
                .findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(testUser.getId());

        assertThat(found).hasSize(3);
        assertThat(found.get(0).getName()).isEqualTo("First");
        assertThat(found.get(1).getName()).isEqualTo("Second");
        assertThat(found.get(2).getName()).isEqualTo("Third");
    }

    @Test
    void shouldIsolateContextsByUser() {
        User otherUser = userRepository.saveAndFlush(
                User.builder()
                        .email("other@example.com")
                        .passwordHash("$2a$10$hash")
                        .build()
        );
        contextRepository.saveAndFlush(buildContext("My Context", ContextTheme.FORMAL, 0, false));
        contextRepository.saveAndFlush(
                Context.builder()
                        .user(otherUser)
                        .name("Other Context")
                        .theme(ContextTheme.NATURE)
                        .sortOrder(0)
                        .build()
        );

        List<Context> myContexts = contextRepository
                .findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(testUser.getId());
        List<Context> otherContexts = contextRepository
                .findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(otherUser.getId());

        assertThat(myContexts).hasSize(1);
        assertThat(myContexts.get(0).getName()).isEqualTo("My Context");
        assertThat(otherContexts).hasSize(1);
        assertThat(otherContexts.get(0).getName()).isEqualTo("Other Context");
    }

    @Test
    void shouldSetTimestamps_whenPersisting() {
        Context saved = contextRepository.saveAndFlush(
                buildContext("Timestamped", ContextTheme.MINIMALIST, 0, false));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    void shouldPersistAllThemeValues() {
        for (ContextTheme theme : ContextTheme.values()) {
            Context saved = contextRepository.saveAndFlush(
                    buildContext("Theme-" + theme.name(), theme, theme.ordinal(), false));
            assertThat(saved.getTheme()).isEqualTo(theme);
        }

        List<Context> all = contextRepository
                .findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(testUser.getId());
        assertThat(all).hasSize(ContextTheme.values().length);
    }

    @Test
    void shouldMaintainForeignKeyToUser() {
        Context saved = contextRepository.saveAndFlush(
                buildContext("FK Test", ContextTheme.FORMAL, 0, false));

        Context fetched = contextRepository.findById(saved.getId()).orElseThrow();
        assertThat(fetched.getUser().getId()).isEqualTo(testUser.getId());
    }

    private Context buildContext(String name, ContextTheme theme, int sortOrder, boolean deleted) {
        return Context.builder()
                .user(testUser)
                .name(name)
                .theme(theme)
                .sortOrder(sortOrder)
                .isDeleted(deleted)
                .build();
    }
}
