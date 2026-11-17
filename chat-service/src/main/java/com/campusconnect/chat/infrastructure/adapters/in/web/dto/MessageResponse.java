package com.campusconnect.chat.infrastructure.adapters.in.web.dto;

import com.campusconnect.chat.domain.Message;
import java.time.Instant;

public record MessageResponse(
        String id,
        String conversationId,
        Long senderId,
        Long recipientId,
        String content,
        Instant createdAt,
        Instant editedAt,
        boolean deleted) {

    public static MessageResponse from(Message message) {
        return from(message, false);
    }

    public static MessageResponse from(Message message, boolean deleted) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getRecipientId(),
                message.getContent(),
                message.getCreatedAt(),
                message.getEditedAt(),
                deleted);
    }
}
