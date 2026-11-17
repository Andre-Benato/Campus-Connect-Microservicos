package com.campusconnect.chat.infrastructure.adapters.out.http;

public record TokenValidateResponse(Long userId, String email, String name) {}
