package com.campusconnect.chat.infrastructure.adapters.out.messaging;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import com.campusconnect.chat.application.ports.RideRequestAcceptedEvent;
import com.campusconnect.chat.application.RideRequestAcceptedHandler;
/**
 * Consome a fila {@code ride-request-accepted} quando a connection string do Service Bus esta
 * configurada. Sem connection string o bean existe mas nao inicia o processor (dev/local).
 */
@Component
public class AzureServiceBusRideRequestAcceptedListener {

    private static final Logger log =
            LoggerFactory.getLogger(AzureServiceBusRideRequestAcceptedListener.class);

    private final ServiceBusProperties properties;
    private final RideRequestAcceptedHandler handler;
    private final ObjectMapper objectMapper;

    private ServiceBusProcessorClient processor;

    public AzureServiceBusRideRequestAcceptedListener(
            ServiceBusProperties properties,
            RideRequestAcceptedHandler handler,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isConfigured()) {
            log.warn(
                    "AZURE_SERVICEBUS_CONNECTION_STRING vazia; consumer de {} desabilitado",
                    RideRequestAcceptedEvent.TYPE);
            return;
        }

        String queue = properties.getQueueRideRequestAccepted();
        processor =
                new ServiceBusClientBuilder()
                        .connectionString(properties.getConnectionString())
                        .processor()
                        .queueName(queue)
                        .processMessage(this::processMessage)
                        .processError(this::processError)
                        .buildProcessorClient();
        processor.start();
        log.info("Consumer Azure Service Bus iniciado (fila={})", queue);
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        try {
            String body = message.getBody().toString();
            RideRequestAcceptedEvent event =
                    objectMapper.readValue(body, RideRequestAcceptedEvent.class);
            handler.handle(event);
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao processar mensagem Service Bus (messageId={})",
                    message.getMessageId(),
                    ex);
            throw ex;
        }
    }

    private void processError(ServiceBusErrorContext context) {
        log.error(
                "Erro no processor Service Bus (entity={}, errorSource={}): {}",
                context.getEntityPath(),
                context.getErrorSource(),
                context.getException().getMessage(),
                context.getException());
    }

    @PreDestroy
    public void stop() {
        if (processor != null) {
            processor.close();
        }
    }
}
