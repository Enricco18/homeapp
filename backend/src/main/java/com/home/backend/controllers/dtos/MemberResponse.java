package com.home.backend.controllers.dtos;

import com.home.backend.entities.HomeUser;
import com.home.backend.entities.enums.HomeRole;

public record MemberResponse(
        UserResponse user,
        HomeRole role
) {
    public static MemberResponse from(HomeUser membership) {
        return new MemberResponse(UserResponse.from(membership.getUser()), membership.getRole());
    }
}
