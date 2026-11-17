package com.campusconnect.chat.application;

import com.campusconnect.chat.infrastructure.adapters.in.web.dto.ConversationResponse;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.MessageResponse;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.SendMessageRequest;
import com.campusconnect.chat.infrastructure.adapters.in.web.dto.UpdateMessageRequest;
import com.campusconnect.common.ForbiddenOperationException;
import com.campusconnect.common.InvalidRequestException;
import com.campusconnect.common.ResourceNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.campusconnect.chat.domain.ConversationIds;
import com.campusconnect.chat.domain.Message;
import com.campusconnect.chat.infrastructure.adapters.out.persistence.MessageRepository;
@Service
public class MessageService {

    private static final int DEFAULT_HISTORY_LIMIT = 50;
    private static final int MAX_HISTORY_LIMIT = 100;
    private static final String USER_QUEUE_MESSAGES = "/queue/messages";

    private final MessageRepository messageRepository;
    private final MongoTemplate mongoTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(
            MessageRepository messageRepository,
            MongoTemplate mongoTemplate,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.mongoTemplate = mongoTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Persiste a mensagem e publica em {@code /user/queue/messages} do destinatario (se conectado).
     * Usado por REST e STOMP.
     */
    public MessageResponse enviar(Long senderId, SendMessageRequest request) {
        Long recipientId = request.recipientId();
        if (senderId.equals(recipientId)) {
            throw new InvalidRequestException("Nao e permitido enviar mensagem para si mesmo");
        }

        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new InvalidRequestException("O conteudo da mensagem e obrigatorio");
        }

        String conversationId = ConversationIds.of(senderId, recipientId);
        Message message = new Message(conversationId, senderId, recipientId, content);
        MessageResponse response = MessageResponse.from(messageRepository.save(message));
        notificarDestinatario(recipientId, response);
        return response;
    }

    public MessageResponse editar(Long userId, String messageId, UpdateMessageRequest request) {
        Message message = buscarDoRemetente(userId, messageId);
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new InvalidRequestException("O conteudo da mensagem e obrigatorio");
        }

        message.editar(content);
        MessageResponse response = MessageResponse.from(messageRepository.save(message));
        notificarDestinatario(message.getRecipientId(), response);
        return response;
    }

    public void remover(Long userId, String messageId) {
        Message message = buscarDoRemetente(userId, messageId);
        messageRepository.delete(message);
        notificarDestinatario(message.getRecipientId(), MessageResponse.from(message, true));
    }

    /**
     * Historico da conversa entre o autenticado e {@code otherUserId}.
     *
     * <p>Retorno em ordem cronologica ascendente (mais antigas primeiro). O cursor {@code before}
     * busca a pagina anterior (mensagens com {@code createdAt} estritamente anterior).
     */
    public List<MessageResponse> listarHistorico(Long userId, Long otherUserId, Instant before, Integer limit) {
        if (userId.equals(otherUserId)) {
            throw new InvalidRequestException("otherUserId deve ser diferente do usuario autenticado");
        }

        int pageSize = normalizarLimit(limit);
        String conversationId = ConversationIds.of(userId, otherUserId);
        var pageable = PageRequest.of(0, pageSize);

        List<Message> page =
                before == null
                        ? messageRepository.findByConversationIdOrderByCreatedAtDesc(
                                conversationId, pageable)
                        : messageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                                conversationId, before, pageable);

        List<Message> chronological = new ArrayList<>(page);
        Collections.reverse(chronological);
        return chronological.stream().map(MessageResponse::from).toList();
    }

    public List<ConversationResponse> listarConversas(Long userId) {
        Aggregation aggregation =
                Aggregation.newAggregation(
                        Aggregation.match(
                                new Criteria()
                                        .orOperator(
                                                Criteria.where("senderId").is(userId),
                                                Criteria.where("recipientId").is(userId))),
                        Aggregation.sort(Sort.Direction.DESC, "createdAt"),
                        Aggregation.group("conversationId")
                                .first(Aggregation.ROOT)
                                .as("lastMessage"),
                        Aggregation.sort(Sort.Direction.DESC, "lastMessage.createdAt"));

        List<ConversationAggResult> results =
                mongoTemplate
                        .aggregate(aggregation, "messages", ConversationAggResult.class)
                        .getMappedResults();

        return results.stream().map(result -> toConversationResponse(userId, result)).toList();
    }

    private static ConversationResponse toConversationResponse(
            Long userId, ConversationAggResult result) {
        Message last = result.lastMessage();
        Long otherUserId =
                userId.equals(last.getSenderId()) ? last.getRecipientId() : last.getSenderId();
        return new ConversationResponse(
                result.id(),
                otherUserId,
                new ConversationResponse.LastMessageSummary(
                        last.getContent(), last.getSenderId(), last.getCreatedAt()));
    }

    private Message buscarDoRemetente(Long userId, String messageId) {
        Message message =
                messageRepository
                        .findById(messageId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Mensagem " + messageId + " nao encontrada"));
        if (!userId.equals(message.getSenderId())) {
            throw new ForbiddenOperationException(
                    "Apenas o remetente pode alterar ou remover a mensagem");
        }
        return message;
    }

    private void notificarDestinatario(Long recipientId, MessageResponse response) {
        messagingTemplate.convertAndSendToUser(
                String.valueOf(recipientId), USER_QUEUE_MESSAGES, response);
    }

    private static int normalizarLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_HISTORY_LIMIT;
        }
        if (limit < 1 || limit > MAX_HISTORY_LIMIT) {
            throw new InvalidRequestException(
                    "limit deve estar entre 1 e " + MAX_HISTORY_LIMIT);
        }
        return limit;
    }

    /** Resultado da agregacao Mongo: {@code _id} = conversationId, {@code lastMessage} = documento. */
    record ConversationAggResult(String id, Message lastMessage) {}
}
