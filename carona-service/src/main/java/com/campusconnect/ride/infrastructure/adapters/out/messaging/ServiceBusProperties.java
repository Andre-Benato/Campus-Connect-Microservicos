package com.campusconnect.ride.infrastructure.adapters.out.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "campusconnect.servicebus")
public class ServiceBusProperties {

    /**
     * Connection string do namespace Azure Service Bus. Vazia = publisher apenas loga (dev/local).
     */
    private String connectionString = "";

    /** Nome da fila para o evento RideRequestAccepted. */
    private String queueRideRequestAccepted = "ride-request-accepted";

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getQueueRideRequestAccepted() {
        return queueRideRequestAccepted;
    }

    public void setQueueRideRequestAccepted(String queueRideRequestAccepted) {
        this.queueRideRequestAccepted = queueRideRequestAccepted;
    }

    public boolean isConfigured() {
        return connectionString != null && !connectionString.isBlank();
    }
}
