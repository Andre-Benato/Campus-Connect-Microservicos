package com.campusconnect.chat.infrastructure.adapters.in.web;

import com.campusconnect.chat.infrastructure.adapters.in.web.dto.ConversationResponse;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.MessageResponse;
import com.campusconnect.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.chat.application.MessageService;
@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversas", description = "Listagem de conversas e histórico (MongoDB)")
public class ConversationController {

    private final MessageService messageService;

    public ConversationController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    @Operation(summary = "Listar conversas do usuário")
    @ApiResponse(responseCode = "200", description = "Conversas retornadas")
    public ResponseEntity<List<ConversationResponse>> listar(
            @AuthenticationPrincipal AuthenticatedUser usuario) {
        return ResponseEntity.ok(messageService.listarConversas(usuario.id()));
    }

    @GetMapping("/{otherUserId}/messages")
    @Operation(summary = "Histórico com outro usuário")
    @ApiResponse(responseCode = "200", description = "Mensagens retornadas")
    public ResponseEntity<List<MessageResponse>> historico(
            @PathVariable Long otherUserId,
            @RequestParam(required = false) Instant before,
            @RequestParam(required = false) Integer limit,
            @AuthenticationPrincipal AuthenticatedUser usuario) {

        return ResponseEntity.ok(
                messageService.listarHistorico(usuario.id(), otherUserId, before, limit));
    }
}
