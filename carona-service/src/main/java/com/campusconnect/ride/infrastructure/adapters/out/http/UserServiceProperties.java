package com.campusconnect.ride.infrastructure.adapters.out.http;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campusconnect.user-service")
public class UserServiceProperties {

    /** Base URL do user-service, sem barra final (ex.: http://localhost:8081). */
    private String baseUrl = "http://localhost:8081";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
