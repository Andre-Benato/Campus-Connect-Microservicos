package com.campusconnect.ride.infrastructure.adapters.out.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import com.campusconnect.ride.application.ports.RideRequestAcceptedPublisher;
@Configuration
@EnableConfigurationProperties(ServiceBusProperties.class)
public class ServiceBusConfig {

    private static final Logger log = LoggerFactory.getLogger(ServiceBusConfig.class);

    @Bean
    RideRequestAcceptedPublisher rideRequestAcceptedPublisher(
            ServiceBusProperties properties, ObjectMapper objectMapper) {
        if (properties.isConfigured()) {
            log.info(
                    "Azure Service Bus habilitado (fila={})",
                    properties.getQueueRideRequestAccepted());
            return new AzureServiceBusRideRequestAcceptedPublisher(properties, objectMapper);
        }
        log.warn(
                "AZURE_SERVICEBUS_CONNECTION_STRING vazia; eventos RideRequestAccepted apenas serao logados");
        return new LoggingRideRequestAcceptedPublisher();
    }
}
