package com.gtd.backend.notification.service;

import java.util.Map;
import java.util.UUID;

public interface PushNotificationService {

    void sendPush(UUID userId, String title, String body, Map<String, String> data);
}
