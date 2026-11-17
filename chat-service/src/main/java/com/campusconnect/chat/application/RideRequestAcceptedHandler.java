package com.campusconnect.chat.application;

import com.campusconnect.chat.application.MessageService;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.SendMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.campusconnect.chat.application.ports.RideRequestAcceptedEvent;
/**
 * Reage a {@link RideRequestAcceptedEvent}: cria mensagem automatica do motorista ao passageiro.
 */
@Service
public class RideRequestAcceptedHandler {

    private static final Logger log = LoggerFactory.getLogger(RideRequestAcceptedHandler.class);

    private final MessageService messageService;

    public RideRequestAcceptedHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    public void handle(RideRequestAcceptedEvent event) {
        String content =
                "Carona aceita: "
                        + event.origin()
                        + " → "
                        + event.destination()
                        + ". Combine os detalhes por aqui.";

        messageService.enviar(
                event.driverUserId(), new SendMessageRequest(event.passengerUserId(), content));

        log.info(
                "Mensagem automatica criada apos {} (rideId={}, requestId={}, driver={}, passenger={})",
                RideRequestAcceptedEvent.TYPE,
                event.rideId(),
                event.requestId(),
                event.driverUserId(),
                event.passengerUserId());
    }
}
