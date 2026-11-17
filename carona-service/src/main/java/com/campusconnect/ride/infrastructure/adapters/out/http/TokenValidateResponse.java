package com.campusconnect.ride.infrastructure.adapters.out.http;

public record TokenValidateResponse(Long userId, String email, String name) {}
