package com.home.backend.entities;

import com.home.backend.entities.enums.HomeRole;
import com.home.backend.entities.enums.InviteStatus;
import com.home.backend.exceptions.DomainException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
public class Invite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne//(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Home home;

    // quem convidou ("from")
    @ManyToOne//(fetch = FetchType.LAZY, optional = false)
    //@JoinColumn(name = "inviter_id")
    private User inviter;

    // quem foi convidado ("to")
    @ManyToOne//(fetch = FetchType.LAZY, optional = false)
    //@JoinColumn(name = "invitee_id")
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status = InviteStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant respondedAt;

    protected Invite() {
    }

    // quem pode convidar (HomePermission.INVITE) é checado no service; aqui só as regras do convite
    public Invite(Home home, User inviter, User invitee, Instant expiresAt) {
        if (invitee.getId().equals(inviter.getId())) {
            throw DomainException.invalid("You cannot invite yourself");
        }
        if (home.isMember(invitee.getId())) {
            throw DomainException.conflict("User is already a member of this home");
        }
        this.home = home;
        this.inviter = inviter;
        this.invitee = invitee;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // ---------- regras ----------

    // aceita e entra na casa como MEMBER
    public void accept(String userId) {
        ensureCanRespond(userId);
        if (!home.isMember(invitee.getId())) {
            home.addMember(invitee, HomeRole.MEMBER); // salvo via cascade da Home
        }
        this.status = InviteStatus.ACCEPTED;
        this.respondedAt = Instant.now();
    }

    public void decline(String userId) {
        ensureCanRespond(userId);
        this.status = InviteStatus.DECLINED;
        this.respondedAt = Instant.now();
    }

    // só convite pendente pode ser cancelado (quem pode cancelar é checado no service)
    public void ensureCanBeDeleted() {
        ensurePending();
    }

    // só o convidado responde: é regra do próprio convite, não de papel na casa.
    // pra outras pessoas o convite "não existe"
    private void ensureCanRespond(String userId) {
        if (!invitee.getId().equals(userId)) {
            throw DomainException.notFound("Invite not found");
        }
        ensurePending();
        if (isExpired()) {
            throw DomainException.gone("Invite has expired");
        }
    }

    private void ensurePending() {
        if (status != InviteStatus.PENDING) {
            throw DomainException.conflict("Invite was already answered");
        }
    }

    public Long getId() {
        return id;
    }

    public Home getHome() {
        return home;
    }

    public User getInviter() {
        return inviter;
    }

    public User getInvitee() {
        return invitee;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}