package com.home.backend.controllers.dtos;

import com.home.backend.entities.HomeUser;
import com.home.backend.entities.enums.HomeRole;

// role = papel do usuário logado nessa casa
public record HomeResponse(
        Long id,
        String name,
        HomeRole role
) {
    public static HomeResponse from(HomeUser membership) {
        return new HomeResponse(
                membership.getHome().getId(),
                membership.getHome().getName(),
                membership.getRole()
        );
    }
}