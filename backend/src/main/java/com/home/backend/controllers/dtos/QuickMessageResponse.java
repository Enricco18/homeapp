package com.home.backend.controllers.dtos;

import com.home.backend.entities.QuickMessage;

import java.time.Instant;

public record QuickMessageResponse(
        Long id,
        String message,
        UserResponse author,
        Instant createdAt,
        Instant expiresAt
) {
    public static QuickMessageResponse from(QuickMessage quickMessage) {
        return new QuickMessageResponse(
                quickMessage.getId(),
                quickMessage.getMessage(),
                UserResponse.from(quickMessage.getAuthor().getUser()),
                quickMessage.getCreatedAt(),
                quickMessage.getExpiresAt()
        );
    }
}
