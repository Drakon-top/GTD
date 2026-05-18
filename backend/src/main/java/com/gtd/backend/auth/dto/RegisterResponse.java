package com.gtd.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registration response with created user details")
public class RegisterResponse {

    @Schema(description = "Unique user identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "User email address", example = "user@example.com")
    private String email;

    @Schema(description = "Account creation timestamp")
    private Instant createdAt;
}
