package com.gtd.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "cors")
@Getter
@Setter
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:5173");
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = List.of("Authorization", "Content-Type", "X-Requested-With");
    private boolean allowCredentials = true;
    private long maxAge = 3600;
}
