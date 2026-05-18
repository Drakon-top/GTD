package com.gtd.backend.notification.dto;

import com.gtd.backend.notification.model.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenResponse {

    private UUID id;
    private String token;
    private DeviceType deviceType;
    private String deviceName;
    private boolean active;
    private Instant createdAt;
}
