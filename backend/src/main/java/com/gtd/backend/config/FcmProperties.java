package com.gtd.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fcm")
public class FcmProperties {

    private boolean enabled = false;
    private String credentialsFile = "";
}
