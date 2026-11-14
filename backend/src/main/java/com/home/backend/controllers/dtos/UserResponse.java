package com.home.backend.controllers.dtos;

import com.home.backend.entities.User;

public record UserResponse(
        String id,
        String username,
        String fullName,
        String email,
        String imageUrl
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getImageUrl()
        );
    }
}
