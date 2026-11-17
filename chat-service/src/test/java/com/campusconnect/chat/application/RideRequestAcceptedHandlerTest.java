package com.campusconnect.chat.application;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.campusconnect.chat.application.MessageService;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.SendMessageRequest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.campusconnect.chat.application.ports.RideRequestAcceptedEvent;
@ExtendWith(MockitoExtension.class)
class RideRequestAcceptedHandlerTest {

    @Mock
    private MessageService messageService;

    private RideRequestAcceptedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RideRequestAcceptedHandler(messageService);
    }

    @Test
    void deveEnviarMensagemDoMotoristaAoPassageiro() {
        RideRequestAcceptedEvent event =
                new RideRequestAcceptedEvent(
                        100L, 200L, 10L, 20L, "Centro", "Campus", 1, Instant.parse("2026-11-04T12:00:00Z"));

        handler.handle(event);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(messageService).enviar(eq(10L), captor.capture());

        SendMessageRequest request = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(request.recipientId()).isEqualTo(20L);
        org.assertj.core.api.Assertions.assertThat(request.content())
                .contains("Centro")
                .contains("Campus")
                .contains("Carona aceita");
    }
}
