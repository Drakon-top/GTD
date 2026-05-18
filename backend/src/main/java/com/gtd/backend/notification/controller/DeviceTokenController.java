package com.gtd.backend.notification.controller;

import com.gtd.backend.notification.dto.DeviceTokenResponse;
import com.gtd.backend.notification.dto.RegisterDeviceTokenRequest;
import com.gtd.backend.notification.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Device Tokens", description = "FCM device token management for push notifications")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/token")
    @Operation(summary = "Register FCM device token", description = "Register or re-activate a device token for push notifications")
    public ResponseEntity<DeviceTokenResponse> registerToken(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        DeviceTokenResponse response = deviceTokenService.registerToken(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/token")
    @Operation(summary = "Unregister FCM device token", description = "Deactivate a device token to stop receiving push notifications")
    public ResponseEntity<Void> unregisterToken(
            @AuthenticationPrincipal UUID userId,
            @RequestParam String token) {
        deviceTokenService.unregisterToken(userId, token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tokens")
    @Operation(summary = "Get user's device tokens", description = "List all active device tokens for the current user")
    public ResponseEntity<List<DeviceTokenResponse>> getUserTokens(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(deviceTokenService.getUserTokens(userId));
    }
}
