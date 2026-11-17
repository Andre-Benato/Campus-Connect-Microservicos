package com.campusconnect.user.infrastructure.adapters.in.web.dto;

import com.campusconnect.user.domain.User;
import java.time.LocalDateTime;

/** Representacao publica do usuario. O hash da senha nunca e exposto. */
public record UserResponse(Long id, String name, String email, LocalDateTime createAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreateAt());
    }
}
