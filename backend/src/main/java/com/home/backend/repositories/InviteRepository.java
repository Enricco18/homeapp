package com.home.backend.repositories;

import com.home.backend.entities.Invite;
import com.home.backend.entities.enums.InviteStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface InviteRepository extends JpaRepository<Invite, Long> {

    // convites recebidos que ainda podem ser respondidos
    @EntityGraph(attributePaths = "home")
    List<Invite> findByInviteeIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            String inviteeId, InviteStatus status, Instant now);

    @EntityGraph(attributePaths = "home")
    List<Invite> findByInviterIdOrderByCreatedAtDesc(String inviterId);

    boolean existsByHomeIdAndInviteeIdAndStatusAndExpiresAtAfter(
            Long homeId, String inviteeId, InviteStatus status, Instant now);
}