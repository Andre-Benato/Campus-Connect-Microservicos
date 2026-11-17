package com.campusconnect.chat.infrastructure.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.campusconnect.chat.domain.Message;
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    List<Message> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            String conversationId, Instant before, Pageable pageable);
}
