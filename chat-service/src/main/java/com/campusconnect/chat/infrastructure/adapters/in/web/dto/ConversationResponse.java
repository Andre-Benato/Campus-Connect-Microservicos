package com.campusconnect.chat.infrastructure.adapters.in.web.dto;

import java.time.Instant;

public record ConversationResponse(
        String conversationId, Long otherUserId, LastMessageSummary lastMessage) {

    public record LastMessageSummary(String content, Long senderId, Instant createdAt) {}
}
