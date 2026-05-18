package com.gtd.backend.context.service;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.context.dto.ContextResponse;
import com.gtd.backend.context.dto.CreateContextRequest;
import com.gtd.backend.context.dto.UpdateContextRequest;
import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextLimitExceededException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.repository.ContextRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContextServiceTest {

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ContextService contextService;

    private final UUID userId = UUID.randomUUID();
    private final UUID contextId = UUID.randomUUID();

    @Test
    void shouldReturnContextsForUser() {
        Context context = buildContext(contextId, userId, "Work", ContextTheme.FORMAL);
        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(List.of(context));

        List<ContextResponse> result = contextService.getContexts(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Work");
        assertThat(result.get(0).getTheme()).isEqualTo(ContextTheme.FORMAL);
    }

    @Test
    void shouldReturnEmptyListWhenNoContexts() {
        when(contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId))
                .thenReturn(List.of());

        List<ContextResponse> result = contextService.getContexts(userId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldGetContextById() {
        Context context = buildContext(contextId, userId, "Home", ContextTheme.NATURE);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));

        ContextResponse result = contextService.getContext(contextId, userId);

        assertThat(result.getId()).isEqualTo(contextId);
        assertThat(result.getName()).isEqualTo("Home");
    }

    @Test
    void shouldThrowNotFoundWhenContextDoesNotExist() {
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contextService.getContext(contextId, userId))
                .isInstanceOf(ContextNotFoundException.class);
    }

    @Test
    void shouldThrowAccessDeniedWhenContextBelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId, "Other", ContextTheme.DARK);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));

        assertThatThrownBy(() -> contextService.getContext(contextId, userId))
                .isInstanceOf(ContextAccessDeniedException.class);
    }

    @Test
    void shouldCreateContext() {
        CreateContextRequest request = CreateContextRequest.builder()
                .name("Work")
                .theme(ContextTheme.FORMAL)
                .icon("briefcase")
                .build();

        User user = User.builder().id(userId).build();
        when(contextRepository.countByUserIdAndIsDeletedFalse(userId)).thenReturn(0);
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(contextRepository.save(any(Context.class))).thenAnswer(invocation -> {
            Context ctx = invocation.getArgument(0);
            ctx.setId(contextId);
            ctx.setCreatedAt(Instant.now());
            ctx.setUpdatedAt(Instant.now());
            return ctx;
        });

        ContextResponse result = contextService.createContext(request, userId);

        assertThat(result.getName()).isEqualTo("Work");
        assertThat(result.getTheme()).isEqualTo(ContextTheme.FORMAL);
        assertThat(result.getIcon()).isEqualTo("briefcase");
        assertThat(result.getSortOrder()).isEqualTo(0);
    }

    @Test
    void shouldTrimNameAndIconOnCreate() {
        CreateContextRequest request = CreateContextRequest.builder()
                .name("  Work  ")
                .theme(ContextTheme.FORMAL)
                .icon("  briefcase  ")
                .build();

        User user = User.builder().id(userId).build();
        when(contextRepository.countByUserIdAndIsDeletedFalse(userId)).thenReturn(0);
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(contextRepository.save(any(Context.class))).thenAnswer(invocation -> {
            Context ctx = invocation.getArgument(0);
            ctx.setId(contextId);
            ctx.setCreatedAt(Instant.now());
            ctx.setUpdatedAt(Instant.now());
            return ctx;
        });

        ContextResponse result = contextService.createContext(request, userId);

        assertThat(result.getName()).isEqualTo("Work");
        assertThat(result.getIcon()).isEqualTo("briefcase");
    }

    @Test
    void shouldThrowLimitExceededWhenCreating6thContext() {
        CreateContextRequest request = CreateContextRequest.builder()
                .name("Too Many")
                .theme(ContextTheme.DARK)
                .icon("x")
                .build();

        when(contextRepository.countByUserIdAndIsDeletedFalse(userId)).thenReturn(5);

        assertThatThrownBy(() -> contextService.createContext(request, userId))
                .isInstanceOf(ContextLimitExceededException.class);
        verify(contextRepository, never()).save(any());
    }

    @Test
    void shouldUpdateContextFields() {
        Context context = buildContext(contextId, userId, "Work", ContextTheme.FORMAL);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(contextRepository.save(any(Context.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateContextRequest request = UpdateContextRequest.builder()
                .name("Office")
                .theme(ContextTheme.DARK)
                .sortOrder(3)
                .build();

        ContextResponse result = contextService.updateContext(contextId, request, userId);

        assertThat(result.getName()).isEqualTo("Office");
        assertThat(result.getTheme()).isEqualTo(ContextTheme.DARK);
        assertThat(result.getSortOrder()).isEqualTo(3);
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        Context context = buildContext(contextId, userId, "Work", ContextTheme.FORMAL);
        context.setIcon("briefcase");
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(contextRepository.save(any(Context.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateContextRequest request = UpdateContextRequest.builder()
                .name("Office")
                .build();

        ContextResponse result = contextService.updateContext(contextId, request, userId);

        assertThat(result.getName()).isEqualTo("Office");
        assertThat(result.getTheme()).isEqualTo(ContextTheme.FORMAL);
        assertThat(result.getIcon()).isEqualTo("briefcase");
    }

    @Test
    void shouldSoftDeleteContext() {
        Context context = buildContext(contextId, userId, "Work", ContextTheme.FORMAL);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(contextRepository.save(any(Context.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contextService.deleteContext(contextId, userId);

        assertThat(context.isDeleted()).isTrue();
        verify(contextRepository).save(context);
    }

    @Test
    void shouldThrowNotFoundOnDeleteWhenContextMissing() {
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contextService.deleteContext(contextId, userId))
                .isInstanceOf(ContextNotFoundException.class);
    }

    @Test
    void shouldThrowAccessDeniedOnDeleteWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId, "Other", ContextTheme.DARK);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));

        assertThatThrownBy(() -> contextService.deleteContext(contextId, userId))
                .isInstanceOf(ContextAccessDeniedException.class);
    }

    private Context buildContext(UUID id, UUID ownerId, String name, ContextTheme theme) {
        User user = User.builder().id(ownerId).build();
        return Context.builder()
                .id(id)
                .user(user)
                .name(name)
                .theme(theme)
                .icon("icon")
                .sortOrder(0)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
