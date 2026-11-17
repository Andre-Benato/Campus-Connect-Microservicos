package com.campusconnect.chat.infrastructure.adapters.in.web;

import com.campusconnect.chat.infrastructure.adapters.in.web.dto.SendMessageRequest;
import com.campusconnect.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.campusconnect.chat.application.MessageService;
/**
 * Entrada STOMP de envio. Reutiliza {@link MessageService#enviar(Long, SendMessageRequest)} —
 * mesma logica do {@code POST /api/v1/messages}. O push ao destinatario fica no service.
 */
@Controller
public class ChatStompController {

    private final MessageService messageService;

    public ChatStompController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/chat.send")
    public void enviar(@Valid @Payload SendMessageRequest request, Principal principal) {
        AuthenticatedUser usuario = requireUser(principal);
        messageService.enviar(usuario.id(), request);
    }

    private static AuthenticatedUser requireUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        throw new org.springframework.messaging.MessagingException("Usuario STOMP nao autenticado");
    }
}
