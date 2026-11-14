package com.home.backend.controllers;

import com.home.backend.controllers.dtos.CreateInviteRequest;
import com.home.backend.controllers.dtos.InviteResponse;
import com.home.backend.exceptions.DomainException;
import com.home.backend.services.InviteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invites")
public class InviteController {
    @Autowired
    private InviteService inviteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InviteResponse createInvite(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody CreateInviteRequest request) {
        return inviteService.createInvite(request.homeId(), request.inviteeId(), jwt.getSubject());
    }

    // GET /invites?type=received -> convites "to" (recebidos)
    // GET /invites?type=sent     -> convites "from" (enviados)
    @GetMapping
    public List<InviteResponse> getInvites(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam String type) {
        return switch (type) {
            case "received" -> inviteService.getReceivedInvites(jwt.getSubject());
            case "sent" -> inviteService.getSentInvites(jwt.getSubject());
            default -> throw DomainException.invalid("type must be 'received' or 'sent'");
        };
    }

    @PostMapping("/{id}/accept")
    public InviteResponse acceptInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return inviteService.acceptInvite(id, jwt.getSubject());
    }

    @PostMapping("/{id}/decline")
    public InviteResponse declineInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return inviteService.declineInvite(id, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvite(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        inviteService.deleteInvite(id, jwt.getSubject());
    }
}