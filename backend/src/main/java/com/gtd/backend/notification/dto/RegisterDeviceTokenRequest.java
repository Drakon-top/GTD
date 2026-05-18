package com.gtd.backend.notification.dto;

import com.gtd.backend.notification.model.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceTokenRequest {

    @NotBlank(message = "Token is required")
    @Size(max = 500, message = "Token must not exceed 500 characters")
    private String token;

    @NotNull(message = "Device type is required")
    private DeviceType deviceType;

    @Size(max = 100, message = "Device name must not exceed 100 characters")
    private String deviceName;
}
