package com.home.backend.services;

import com.home.backend.controllers.dtos.InviteResponse;
import com.home.backend.entities.HomeUser;
import com.home.backend.entities.Invite;
import com.home.backend.entities.User;
import com.home.backend.entities.enums.HomePermission;
import com.home.backend.entities.enums.InviteStatus;
import com.home.backend.entities.identifiers.HomeUserId;
import com.home.backend.exceptions.DomainException;
import com.home.backend.repositories.HomeUserRepository;
import com.home.backend.repositories.InviteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Acesso por papel na casa (convidar, cancelar convite de outros) é checado aqui.
 * Regras do próprio convite (não convidar a si mesmo, só o convidado responde, pendente, expirado)
 * ficam no Invite.
 */
@Service
public class InviteService {
    private static final Duration INVITE_TTL = Duration.ofDays(7);

    @Autowired
    private InviteRepository inviteRepository;

    @Autowired
    private HomeUserRepository homeUserRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public InviteResponse createInvite(Long homeId, String inviteeId, String inviterId) {
        HomeUser inviter = homeUserRepository.findById(new HomeUserId(homeId, inviterId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        inviter.require(HomePermission.INVITE);

        if (inviteRepository.existsByHomeIdAndInviteeIdAndStatusAndExpiresAtAfter(
                homeId, inviteeId, InviteStatus.PENDING, Instant.now())) {
            throw DomainException.conflict("User already has a pending invite to this home");
        }

        User invitee = userService.syncFromClerk(inviteeId); // 404 se não existe no Clerk
        Invite invite = new Invite(inviter.getHome(), inviter.getUser(), invitee, Instant.now().plus(INVITE_TTL));

        return InviteResponse.from(inviteRepository.save(invite));
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getReceivedInvites(String userId) {
        return inviteRepository
                .findByInviteeIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(userId, InviteStatus.PENDING, Instant.now())
                .stream()
                .map(InviteResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InviteResponse> getSentInvites(String userId) {
        return inviteRepository.findByInviterIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(InviteResponse::from)
                .toList();
    }

    @Transactional
    public InviteResponse acceptInvite(Long inviteId, String userId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> DomainException.notFound("Invite not found"));
        invite.accept(userId);
        return InviteResponse.from(invite);
    }

    @Transactional
    public InviteResponse declineInvite(Long inviteId, String userId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> DomainException.notFound("Invite not found"));
        invite.decline(userId);
        return InviteResponse.from(invite);
    }

    // quem convidou pode cancelar o próprio convite; o de outras pessoas exige MANAGE_HOME
    @Transactional
    public void deleteInvite(Long inviteId, String userId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> DomainException.notFound("Invite not found"));

        boolean isInviter = invite.getInviter().getId().equals(userId);
        boolean canManageHome = homeUserRepository.findById(new HomeUserId(invite.getHome().getId(), userId))
                .map(member -> member.can(HomePermission.MANAGE_HOME))
                .orElse(false);
        if (!isInviter && !canManageHome) {
            throw DomainException.forbidden("You cannot delete this invite");
        }

        invite.ensureCanBeDeleted();
        inviteRepository.delete(invite);
    }
}
