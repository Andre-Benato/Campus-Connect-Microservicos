package com.campusconnect.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusconnect.chat.infrastructure.adapters.in.web.dto.MessageResponse;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.SendMessageRequest;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.UpdateMessageRequest;
import com.campusconnect.common.ForbiddenOperationException;
import com.campusconnect.common.InvalidRequestException;
import com.campusconnect.common.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.campusconnect.chat.domain.Message;
import com.campusconnect.chat.infrastructure.adapters.out.persistence.MessageRepository;
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(messageRepository, mongoTemplate, messagingTemplate);
    }

    @Test
    void deveRecusarEnvioParaSiMesmo() {
        assertThatThrownBy(() -> messageService.enviar(1L, new SendMessageRequest(1L, "oi")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("si mesmo");
        verify(messageRepository, never()).save(any());
    }

    @Test
    void deveRecusarConteudoEmBranco() {
        assertThatThrownBy(() -> messageService.enviar(1L, new SendMessageRequest(2L, "   ")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("conteudo");
    }

    @Test
    void devePersistirMensagemComConversationIdCanonicoENotificarDestinatario() {
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(
                        invocation -> {
                            Message saved = invocation.getArgument(0);
                            ReflectionTestUtils.setField(saved, "id", "msg-1");
                            return saved;
                        });

        MessageResponse response = messageService.enviar(5L, new SendMessageRequest(2L, "  Ola  "));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message persisted = messageCaptor.getValue();

        assertThat(persisted.getConversationId()).isEqualTo("2_5");
        assertThat(persisted.getContent()).isEqualTo("Ola");
        assertThat(persisted.getSenderId()).isEqualTo(5L);
        assertThat(persisted.getRecipientId()).isEqualTo(2L);

        assertThat(response.id()).isEqualTo("msg-1");
        assertThat(response.conversationId()).isEqualTo("2_5");

        verify(messagingTemplate)
                .convertAndSendToUser(eq("2"), eq("/queue/messages"), eq(response));
    }

    @Test
    void deveRecusarHistoricoQuandoOtherUserIdEhOProprioUsuario() {
        assertThatThrownBy(() -> messageService.listarHistorico(3L, 3L, null, 10))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("otherUserId");
    }

    @Test
    void deveListarHistoricoEmOrdemCronologicaAscendente() {
        Instant agora = Instant.parse("2026-11-04T12:00:00Z");
        Message maisNova = new Message("1_2", 1L, 2L, "depois");
        ReflectionTestUtils.setField(maisNova, "id", "b");
        ReflectionTestUtils.setField(maisNova, "createdAt", agora);
        Message maisAntiga = new Message("1_2", 2L, 1L, "antes");
        ReflectionTestUtils.setField(maisAntiga, "id", "a");
        ReflectionTestUtils.setField(maisAntiga, "createdAt", agora.minusSeconds(60));

        when(messageRepository.findByConversationIdOrderByCreatedAtDesc(
                        eq("1_2"), any(Pageable.class)))
                .thenReturn(List.of(maisNova, maisAntiga));

        List<MessageResponse> historico = messageService.listarHistorico(1L, 2L, null, 10);

        assertThat(historico).extracting(MessageResponse::content).containsExactly("antes", "depois");
    }

    @Test
    void deveRecusarLimitForaDoIntervalo() {
        assertThatThrownBy(() -> messageService.listarHistorico(1L, 2L, null, 0))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("limit");
        assertThatThrownBy(() -> messageService.listarHistorico(1L, 2L, null, 101))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void deveAtualizarConteudoQuandoRemetenteEdita() {
        Message existente = mensagemPersistida("msg-1", 5L, 2L, "Ola");
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(existente));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response =
                messageService.editar(5L, "msg-1", new UpdateMessageRequest("  Ola editado  "));

        assertThat(response.content()).isEqualTo("Ola editado");
        assertThat(response.editedAt()).isNotNull();
        assertThat(response.deleted()).isFalse();
        verify(messagingTemplate)
                .convertAndSendToUser(eq("2"), eq("/queue/messages"), eq(response));
    }

    @Test
    void deveRecusarEdicaoComConteudoEmBranco() {
        Message existente = mensagemPersistida("msg-1", 5L, 2L, "Ola");
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(existente));

        assertThatThrownBy(
                        () -> messageService.editar(5L, "msg-1", new UpdateMessageRequest("   ")))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("conteudo");
        verify(messageRepository, never()).save(any());
    }

    @Test
    void deveRecusarEdicaoQuandoUsuarioNaoERemetente() {
        Message existente = mensagemPersistida("msg-1", 5L, 2L, "Ola");
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(existente));

        assertThatThrownBy(
                        () -> messageService.editar(2L, "msg-1", new UpdateMessageRequest("hack")))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void deveLancarNotFoundQuandoMensagemNaoExisteParaEdicao() {
        when(messageRepository.findById("sumiu")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> messageService.editar(1L, "sumiu", new UpdateMessageRequest("oi")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("sumiu");
    }

    @Test
    void deveRemoverQuandoRemetenteExclui() {
        Message existente = mensagemPersistida("msg-1", 5L, 2L, "Ola");
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(existente));

        messageService.remover(5L, "msg-1");

        verify(messageRepository).delete(existente);
        ArgumentCaptor<MessageResponse> captor = ArgumentCaptor.forClass(MessageResponse.class);
        verify(messagingTemplate)
                .convertAndSendToUser(eq("2"), eq("/queue/messages"), captor.capture());
        assertThat(captor.getValue().id()).isEqualTo("msg-1");
        assertThat(captor.getValue().deleted()).isTrue();
    }

    @Test
    void deveRecusarRemocaoQuandoUsuarioNaoERemetente() {
        Message existente = mensagemPersistida("msg-1", 5L, 2L, "Ola");
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> messageService.remover(2L, "msg-1"))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(messageRepository, never()).delete(any());
    }

    @Test
    void deveLancarNotFoundQuandoMensagemNaoExisteParaRemocao() {
        when(messageRepository.findById("sumiu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.remover(1L, "sumiu"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("sumiu");
    }

    private static Message mensagemPersistida(
            String id, Long senderId, Long recipientId, String content) {
        Message message = new Message("2_5", senderId, recipientId, content);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }
}
