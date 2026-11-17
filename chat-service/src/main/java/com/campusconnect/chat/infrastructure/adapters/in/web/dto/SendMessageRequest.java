package com.campusconnect.chat.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotNull @Positive Long recipientId,
        @NotBlank @Size(max = 2000) String content) {}
