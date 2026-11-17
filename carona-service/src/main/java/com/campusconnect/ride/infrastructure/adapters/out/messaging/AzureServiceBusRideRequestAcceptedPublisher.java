package com.campusconnect.ride.infrastructure.adapters.out.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import tools.jackson.databind.ObjectMapper;

import com.campusconnect.ride.application.ports.RideRequestAcceptedEvent;
import com.campusconnect.ride.application.ports.RideRequestAcceptedPublisher;
public class AzureServiceBusRideRequestAcceptedPublisher
        implements RideRequestAcceptedPublisher, DisposableBean {

    private static final Logger log =
            LoggerFactory.getLogger(AzureServiceBusRideRequestAcceptedPublisher.class);

    private final ServiceBusSenderClient sender;
    private final ObjectMapper objectMapper;
    private final String queueName;

    public AzureServiceBusRideRequestAcceptedPublisher(
            ServiceBusProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.queueName = properties.getQueueRideRequestAccepted();
        this.sender =
                new ServiceBusClientBuilder()
                        .connectionString(properties.getConnectionString())
                        .sender()
                        .queueName(queueName)
                        .buildClient();
    }

    @Override
    public void publish(RideRequestAcceptedEvent event) {
        try {
            String body = objectMapper.writeValueAsString(event);
            ServiceBusMessage message =
                    new ServiceBusMessage(body)
                            .setContentType("application/json")
                            .setSubject(RideRequestAcceptedEvent.TYPE);
            sender.sendMessage(message);
            log.info(
                    "Evento {} publicado na fila {} (rideId={}, requestId={})",
                    RideRequestAcceptedEvent.TYPE,
                    queueName,
                    event.rideId(),
                    event.requestId());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Falha ao publicar " + RideRequestAcceptedEvent.TYPE + " no Service Bus", ex);
        }
    }

    @Override
    public void destroy() {
        sender.close();
    }
}
