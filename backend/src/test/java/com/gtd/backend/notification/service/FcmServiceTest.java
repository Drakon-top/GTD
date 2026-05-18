package com.gtd.backend.notification.service;

import com.google.firebase.messaging.*;
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

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @InjectMocks
    private FcmService fcmService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void shouldSkipSend_whenNoActiveTokens() {
        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(Collections.emptyList());

        fcmService.sendToUser(userId, "Title", "Body", Map.of("key", "value"));

        verifyNoInteractions(firebaseMessaging);
    }

    @Test
    void shouldSendMulticast_whenTokensExist() throws Exception {
        DeviceToken dt = DeviceToken.builder()
                .token("fcm-token-1")
                .deviceType(DeviceType.ANDROID)
                .active(true)
                .build();

        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(dt));

        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(1);
        when(batchResponse.getFailureCount()).thenReturn(0);
        when(batchResponse.getResponses()).thenReturn(List.of(successResponse));

        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

        fcmService.sendToUser(userId, "Test Title", "Test Body", Map.of("taskId", "123"));

        verify(firebaseMessaging).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    void shouldDeactivateUnregisteredTokens() throws Exception {
        DeviceToken dt = DeviceToken.builder()
                .id(UUID.randomUUID())
                .token("invalid-token")
                .deviceType(DeviceType.ANDROID)
                .active(true)
                .build();

        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(dt));

        FirebaseMessagingException fme = mock(FirebaseMessagingException.class);
        when(fme.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);

        SendResponse failResponse = mock(SendResponse.class);
        when(failResponse.isSuccessful()).thenReturn(false);
        when(failResponse.getException()).thenReturn(fme);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(0);
        when(batchResponse.getFailureCount()).thenReturn(1);
        when(batchResponse.getResponses()).thenReturn(List.of(failResponse));

        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);
        when(deviceTokenRepository.findByToken("invalid-token")).thenReturn(Optional.of(dt));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenAnswer(inv -> inv.getArgument(0));

        fcmService.sendToUser(userId, "Title", "Body", Map.of());

        assertFalse(dt.isActive());
        verify(deviceTokenRepository).save(dt);
    }

    @Test
    void shouldThrowFcmSendException_whenFirebaseThrows() throws Exception {
        DeviceToken dt = DeviceToken.builder()
                .token("token-1")
                .deviceType(DeviceType.ANDROID)
                .active(true)
                .build();

        when(deviceTokenRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(dt));
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                .thenThrow(mock(FirebaseMessagingException.class));

        assertThrows(FcmService.FcmSendException.class,
                () -> fcmService.sendToUser(userId, "Title", "Body", Map.of()));
    }
}
