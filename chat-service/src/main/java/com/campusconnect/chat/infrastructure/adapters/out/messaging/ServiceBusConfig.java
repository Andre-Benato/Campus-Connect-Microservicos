package com.campusconnect.chat.infrastructure.adapters.out.messaging;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ServiceBusProperties.class)
public class ServiceBusConfig {}
