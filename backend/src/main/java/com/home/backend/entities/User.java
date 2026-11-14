package com.home.backend.entities;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users") // "user" é palavra reservada no Postgres
public class User {
    // id vem do Clerk, por isso não é gerado
    @Id
    private String id;

    // perfil copiado dos claims do token do Clerk (sincronizado pelo UserSyncInterceptor).
    // nullable: convidado que nunca logou só tem o id até o primeiro request dele.
    // sem unique: o Clerk já garante, e a cópia local pode ficar duplicada por instantes
    private String username;

    private String fullName;

    private String email;

    private String imageUrl;

    @OneToMany(mappedBy = "user")
    private Set<HomeUser> memberships = new HashSet<>();

    protected User() {
    }

    public User(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Set<HomeUser> getMemberships() {
        return memberships;
    }
}