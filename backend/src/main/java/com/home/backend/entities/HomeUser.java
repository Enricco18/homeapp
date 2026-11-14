package com.home.backend.entities;

import com.home.backend.entities.enums.HomePermission;
import com.home.backend.entities.enums.HomeRole;
import com.home.backend.exceptions.DomainException;
import com.home.backend.entities.identifiers.HomeUserId;
import jakarta.persistence.*;

@Entity
public class HomeUser {
    // PK composta (home_id, user_id): já garante que o usuário entra uma vez só em cada casa
    @EmbeddedId
    private HomeUserId id;

    @MapsId("homeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Home home;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HomeRole role;

    protected HomeUser() {
    }

    public HomeUser(Home home, User user, HomeRole role) {
        this.home = home;
        this.user = user;
        this.role = role;
    }

    // ---------- permissões ----------

    public boolean can(HomePermission permission) {
        return role.can(permission);
    }

    // 403 se o papel desse membro não permitir a ação
    public void require(HomePermission permission) {
        if (!can(permission)) {
            throw DomainException.forbidden("You don't have permission to do this");
        }
    }

    public HomeUserId getId() {
        return id;
    }

    public Home getHome() {
        return home;
    }

    public User getUser() {
        return user;
    }

    public HomeRole getRole() {
        return role;
    }

    public void setRole(HomeRole role) {
        this.role = role;
    }
}