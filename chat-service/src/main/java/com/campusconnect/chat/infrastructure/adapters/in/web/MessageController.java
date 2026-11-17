package com.campusconnect.chat.infrastructure.adapters.in.web;

import com.campusconnect.chat.infrastructure.adapters.in.web.dto.MessageResponse;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.SendMessageRequest;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.UpdateMessageRequest;
import com.campusconnect.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.chat.application.MessageService;
@RestController
@RequestMapping("/api/v1/messages")
@Tag(name = "Mensagens", description = "Envio, edicao e exclusao de mensagens do chat (MongoDB)")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Enviar mensagem")
    @ApiResponse(responseCode = "201", description = "Mensagem persistida")
    public ResponseEntity<MessageResponse> enviar(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {

        MessageResponse criada = messageService.enviar(usuario.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/messages/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar propria mensagem")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mensagem atualizada"),
        @ApiResponse(responseCode = "403", description = "Sem permissao"),
        @ApiResponse(responseCode = "404", description = "Mensagem nao encontrada")
    })
    public MessageResponse editar(
            @PathVariable String id,
            @Valid @RequestBody UpdateMessageRequest request,
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return messageService.editar(usuario.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir propria mensagem")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Mensagem removida"),
        @ApiResponse(responseCode = "403", description = "Sem permissao"),
        @ApiResponse(responseCode = "404", description = "Mensagem nao encontrada")
    })
    public ResponseEntity<Void> remover(
            @PathVariable String id, @AuthenticationPrincipal AuthenticatedUser usuario) {
        messageService.remover(usuario.id(), id);
        return ResponseEntity.noContent().build();
    }
}
