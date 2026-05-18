package com.gtd.backend.notification.service;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.notification.dto.DeviceTokenResponse;
import com.gtd.backend.notification.dto.RegisterDeviceTokenRequest;
import com.gtd.backend.notification.model.DeviceToken;
import com.gtd.backend.notification.model.DeviceType;
import com.gtd.backend.notification.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().id(userId).email("test@example.com").passwordHash("hash").build();
    }

    @Test
    void shouldRegisterNewToken() {
        RegisterDeviceTokenRequest request = RegisterDeviceTokenRequest.builder()
                .token("fcm-token-123")
                .deviceType(DeviceType.ANDROID)
                .deviceName("Pixel 8")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByToken("fcm-token-123")).thenReturn(Optional.empty());
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> {
            DeviceToken dt = inv.getArgument(0);
            dt.setId(UUID.randomUUID());
            dt.setCreatedAt(Instant.now());
            dt.setUpdatedAt(Instant.now());
            return dt;
        });

        DeviceTokenResponse response = deviceTokenService.registerToken(userId, request);

        assertNotNull(response);
        assertEquals("fcm-token-123", response.getToken());
        assertEquals(DeviceType.ANDROID, response.getDeviceType());
        assertEquals("Pixel 8", response.getDeviceName());
        assertTrue(response.isActive());

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUser().getId());
    }

    @Test
    void shouldReactivateExistingToken() {
        RegisterDeviceTokenRequest request = RegisterDeviceTokenRequest.builder()
                .token("existing-token")
                .deviceType(DeviceType.WEB)
                .deviceName("Chrome")
                .build();

        DeviceToken existing = DeviceToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("existing-token")
                .deviceType(DeviceType.ANDROID)
                .active(false)
                .build();
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(deviceTokenRepository.findByToken("existing-token")).thenReturn(Optional.of(existing));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        DeviceTokenResponse response = deviceTokenService.registerToken(userId, request);

        assertTrue(response.isActive());
        assertEquals(DeviceType.WEB, response.getDeviceType());
        assertEquals("Chrome", response.getDeviceName());
    }

    @Test
    void shouldThrow_whenUserNotFound() {
        RegisterDeviceTokenRequest request = RegisterDeviceTokenRequest.builder()
                .token("token")
                .deviceType(DeviceType.ANDROID)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> deviceTokenService.registerToken(userId, request));
    }

    @Test
    void shouldUnregisterToken() {
        DeviceToken dt = DeviceToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("token-to-remove")
                .active(true)
                .build();

        when(deviceTokenRepository.findByToken("token-to-remove")).thenReturn(Optional.of(dt));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        deviceTokenService.unregisterToken(userId, "token-to-remove");

        assertFalse(dt.isActive());
        verify(deviceTokenRepository).save(dt);
    }

    @Test
    void shouldNotUnregisterTokenOfOtherUser() {
        UUID otherUserId = UUID.randomUUID();
        User otherUser = User.builder().id(otherUserId).email("other@example.com").passwordHash("hash").build();
        DeviceToken dt = DeviceToken.builder()
                .id(UUID.randomUUID())
                .user(otherUser)
                .token("other-token")
                .active(true)
                .build();

        when(deviceTokenRepository.findByToken("other-token")).thenReturn(Optional.of(dt));

        deviceTokenService.unregisterToken(userId, "other-token");

        assertTrue(dt.isActive());
        verify(deviceTokenRepository, never()).save(any());
    }

    @Test
    void shouldGetUserTokens() {
        DeviceToken dt1 = DeviceToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("token-1")
                .deviceType(DeviceType.ANDROID)
                .deviceName("Phone")
                .active(true)
                .build();
        dt1.setCreatedAt(Instant.now());

        DeviceToken dt2 = DeviceToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .token("token-2")
                .deviceType(DeviceType.WEB)
                .deviceName("Browser")
                .active(true)
                .build();
        dt2.setCreatedAt(Instant.now());

        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(dt1, dt2));

        List<DeviceTokenResponse> responses = deviceTokenService.getUserTokens(userId);

        assertEquals(2, responses.size());
        assertEquals("token-1", responses.get(0).getToken());
        assertEquals("token-2", responses.get(1).getToken());
    }
}
