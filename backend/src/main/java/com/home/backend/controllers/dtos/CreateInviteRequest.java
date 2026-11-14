package com.home.backend.controllers.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInviteRequest(
        @NotNull Long homeId,
        @NotBlank String inviteeId
) {
}