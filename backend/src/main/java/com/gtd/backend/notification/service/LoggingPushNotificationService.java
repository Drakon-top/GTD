package com.gtd.backend.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Fallback when FCM is disabled — logs the notification for development/testing.
 */
@Slf4j
@Service
@ConditionalOnMissingBean(FcmPushNotificationService.class)
public class LoggingPushNotificationService implements PushNotificationService {

    @Override
    public void sendPush(UUID userId, String title, String body, Map<String, String> data) {
        log.info("Push notification (FCM disabled): userId={}, title='{}', body='{}', data={}",
                userId, title, body, data);
    }
}
