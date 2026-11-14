package com.home.backend.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
public class QuickMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // autor = membro da casa (FK composta pra home_user): o banco garante que só membro escreve,
  // e se ele sair da casa (ou a casa for apagada) a mensagem vai junto
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumns({
    @JoinColumn(name = "home_id", referencedColumnName = "home_id"),
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
  })
  @OnDelete(action = OnDeleteAction.CASCADE)
  private HomeUser author;

  // mesma coluna home_id, só leitura (quem grava é o author): permite findByHomeId e getHome()
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "home_id", insertable = false, updatable = false)
  private Home home;

  @Column(length = 20, nullable = false)
  private String message;

  @CreationTimestamp
  private Instant createdAt;

  @Column(nullable = false)
  private Instant expiresAt;

  protected QuickMessage() {}

  public QuickMessage(HomeUser author, String message, Instant expiresAt) {
    this.author = author;
    this.home = author.getHome();
    this.message = message;
    this.expiresAt = expiresAt;
  }

  public Long getId() {
    return id;
  }

  public HomeUser getAuthor() {
    return author;
  }

  public Home getHome() {
    return home;
  }

  public String getMessage() {
    return message;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
