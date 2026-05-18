package com.gtd.backend.notification.service;

import com.google.firebase.messaging.*;
import com.gtd.backend.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseMessaging.class)
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    public void sendToUser(UUID userId, String title, String body, Map<String, String> data) {
        var tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) {
            log.debug("No active device tokens for userId={}, skipping push", userId);
            return;
        }

        List<String> tokenStrings = tokens.stream()
                .map(com.gtd.backend.notification.model.DeviceToken::getToken)
                .toList();

        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .addAllTokens(tokenStrings)
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);
            log.info("FCM multicast sent to userId={}: success={}, failure={}",
                    userId, response.getSuccessCount(), response.getFailureCount());

            handleFailedTokens(tokenStrings, response.getResponses());
        } catch (FirebaseMessagingException e) {
            log.error("FCM send failed for userId={}: {}", userId, e.getMessage(), e);
            throw new FcmSendException("Failed to send push notification", e);
        }
    }

    private void handleFailedTokens(List<String> tokens, List<SendResponse> responses) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse resp = responses.get(i);
            if (!resp.isSuccessful() && resp.getException() != null) {
                MessagingErrorCode errorCode = resp.getException().getMessagingErrorCode();
                if (errorCode == MessagingErrorCode.UNREGISTERED
                        || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    String invalidToken = tokens.get(i);
                    log.warn("Deactivating invalid FCM token: errorCode={}, token={}...",
                            errorCode, invalidToken.substring(0, Math.min(10, invalidToken.length())));
                    deviceTokenRepository.findByToken(invalidToken)
                            .ifPresent(dt -> {
                                dt.setActive(false);
                                deviceTokenRepository.save(dt);
                            });
                }
            }
        }
    }

    public static class FcmSendException extends RuntimeException {
        public FcmSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
