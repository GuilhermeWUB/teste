package com.necsus.necsusspring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class ApiBrasilConfig {

    @Value("${apibrasil.bearer_token:}")
    private String bearerToken;

    @Value("${apibrasil.device_token:}")
    private String deviceToken;

    public String getBearerToken() {
        return bearerToken;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public boolean hasValidCredentials() {
        return StringUtils.hasText(bearerToken) && StringUtils.hasText(deviceToken);
    }
}
