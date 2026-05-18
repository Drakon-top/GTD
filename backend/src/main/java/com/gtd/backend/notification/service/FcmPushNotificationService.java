package com.gtd.backend.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FcmPushNotificationService implements PushNotificationService {

    private final FcmService fcmService;

    @Override
    public void sendPush(UUID userId, String title, String body, Map<String, String> data) {
        try {
            fcmService.sendToUser(userId, title, body, data);
        } catch (FcmService.FcmSendException e) {
            log.error("Push notification failed for userId={}, will be retried via RabbitMQ: {}",
                    userId, e.getMessage());
            throw e;
        }
    }
}
