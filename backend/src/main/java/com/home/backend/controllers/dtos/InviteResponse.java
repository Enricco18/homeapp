package com.home.backend.controllers.dtos;

import com.home.backend.entities.Invite;
import com.home.backend.entities.enums.InviteStatus;

import java.time.Instant;

public record InviteResponse(
        Long id,
        Long homeId,
        String homeName,
        UserResponse inviter,
        UserResponse invitee,
        InviteStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant respondedAt
) {
    public static InviteResponse from(Invite invite) {
        return new InviteResponse(
                invite.getId(),
                invite.getHome().getId(),
                invite.getHome().getName(),
                UserResponse.from(invite.getInviter()),
                UserResponse.from(invite.getInvitee()),
                invite.getStatus(),
                invite.getCreatedAt(),
                invite.getExpiresAt(),
                invite.getRespondedAt()
        );
    }
}
