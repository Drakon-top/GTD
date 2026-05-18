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
import com.gtd.backend.context.repository.ContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContextService {

    private static final int MAX_CONTEXTS = 5;

    private final ContextRepository contextRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ContextResponse> getContexts(UUID userId) {
        return contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContextResponse getContext(UUID contextId, UUID userId) {
        Context context = findContextOrThrow(contextId);
        verifyOwnership(context, userId);
        return toResponse(context);
    }

    @Transactional
    public ContextResponse createContext(CreateContextRequest request, UUID userId) {
        int currentCount = contextRepository.countByUserIdAndIsDeletedFalse(userId);
        if (currentCount >= MAX_CONTEXTS) {
            throw new ContextLimitExceededException();
        }

        User user = userRepository.getReferenceById(userId);

        Context context = Context.builder()
                .user(user)
                .name(request.getName().trim())
                .theme(request.getTheme())
                .icon(request.getIcon().trim())
                .sortOrder(currentCount)
                .build();

        Context saved = contextRepository.save(context);
        return toResponse(saved);
    }

    @Transactional
    public ContextResponse updateContext(UUID contextId, UpdateContextRequest request, UUID userId) {
        Context context = findContextOrThrow(contextId);
        verifyOwnership(context, userId);

        if (request.getName() != null) {
            context.setName(request.getName().trim());
        }
        if (request.getTheme() != null) {
            context.setTheme(request.getTheme());
        }
        if (request.getIcon() != null) {
            context.setIcon(request.getIcon().trim());
        }
        if (request.getSortOrder() != null) {
            context.setSortOrder(request.getSortOrder());
        }

        Context saved = contextRepository.save(context);
        return toResponse(saved);
    }

    @Transactional
    public void deleteContext(UUID contextId, UUID userId) {
        Context context = findContextOrThrow(contextId);
        verifyOwnership(context, userId);

        context.setDeleted(true);
        contextRepository.save(context);
    }

    private Context findContextOrThrow(UUID contextId) {
        return contextRepository.findByIdAndIsDeletedFalse(contextId)
                .orElseThrow(() -> new ContextNotFoundException(contextId));
    }

    private void verifyOwnership(Context context, UUID userId) {
        if (!context.getUser().getId().equals(userId)) {
            throw new ContextAccessDeniedException(context.getId());
        }
    }

    private ContextResponse toResponse(Context context) {
        return ContextResponse.builder()
                .id(context.getId())
                .name(context.getName())
                .theme(context.getTheme())
                .icon(context.getIcon())
                .sortOrder(context.getSortOrder())
                .createdAt(context.getCreatedAt())
                .updatedAt(context.getUpdatedAt())
                .build();
    }
}
