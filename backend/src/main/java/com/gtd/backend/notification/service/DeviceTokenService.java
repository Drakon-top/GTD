package com.gtd.backend.notification.service;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.notification.dto.DeviceTokenResponse;
import com.gtd.backend.notification.dto.RegisterDeviceTokenRequest;
import com.gtd.backend.notification.model.DeviceToken;
import com.gtd.backend.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeviceTokenResponse registerToken(UUID userId, RegisterDeviceTokenRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        var existing = deviceTokenRepository.findByToken(request.getToken());
        if (existing.isPresent()) {
            DeviceToken dt = existing.get();
            dt.setActive(true);
            dt.setDeviceName(request.getDeviceName());
            dt.setDeviceType(request.getDeviceType());
            dt.setUser(user);
            deviceTokenRepository.save(dt);
            log.info("Re-activated device token for userId={}, deviceType={}", userId, request.getDeviceType());
            return toResponse(dt);
        }

        DeviceToken deviceToken = DeviceToken.builder()
                .user(user)
                .token(request.getToken())
                .deviceType(request.getDeviceType())
                .deviceName(request.getDeviceName())
                .build();

        deviceToken = deviceTokenRepository.save(deviceToken);
        log.info("Registered new device token for userId={}, deviceType={}", userId, request.getDeviceType());
        return toResponse(deviceToken);
    }

    @Transactional
    public void unregisterToken(UUID userId, String token) {
        deviceTokenRepository.findByToken(token)
                .ifPresent(dt -> {
                    if (dt.getUser().getId().equals(userId)) {
                        dt.setActive(false);
                        deviceTokenRepository.save(dt);
                        log.info("Deactivated device token for userId={}", userId);
                    }
                });
    }

    @Transactional(readOnly = true)
    public List<DeviceTokenResponse> getUserTokens(UUID userId) {
        return deviceTokenRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private DeviceTokenResponse toResponse(DeviceToken dt) {
        return DeviceTokenResponse.builder()
                .id(dt.getId())
                .token(dt.getToken())
                .deviceType(dt.getDeviceType())
                .deviceName(dt.getDeviceName())
                .active(dt.isActive())
                .createdAt(dt.getCreatedAt())
                .build();
    }
}
