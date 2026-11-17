package com.campusconnect.chat.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "idx_conversation_created", def = "{'conversationId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_sender_created", def = "{'senderId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_recipient_created", def = "{'recipientId': 1, 'createdAt': -1}")
})
public class Message {

    @Id
    private String id;

    private String conversationId;
    private Long senderId;
    private Long recipientId;
    private String content;
    private Instant createdAt;
    private Instant editedAt;

    protected Message() {
        // exigido pelo Spring Data
    }

    public Message(String conversationId, Long senderId, Long recipientId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEditedAt() {
        return editedAt;
    }

    public void editar(String novoConteudo) {
        this.content = novoConteudo;
        this.editedAt = Instant.now();
    }
}
