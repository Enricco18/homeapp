package com.home.backend.entities;

import com.home.backend.exceptions.DomainException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

// uma linha da lista de compras da casa.
// regra: no máximo uma linha pendente (bought = false) por item; depois de comprada, a linha não muda mais
@Entity
public class GroceryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FKs com ON DELETE CASCADE: apagar a casa ou o item apaga a linha no próprio banco
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Home home;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Item item;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean bought = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bought_by_id")
    private User boughtBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // como a compra é a última edição possível, depois do bought isso é o momento da compra
    @UpdateTimestamp
    private Instant updatedAt;

    protected GroceryItem() {
    }

    public GroceryItem(Home home, Item item, int quantity) {
        this.home = home;
        this.item = item;
        this.quantity = quantity;
    }

    // ---------- regras do dado: depois de comprada, a linha não muda mais ----------
    // (quem pode mexer é decidido no service; apagar vale mesmo depois de comprada)

    public void addQuantity(int amount) {
        ensureNotBought();
        this.quantity += amount;
    }

    public void changeQuantity(int quantity) {
        ensureNotBought();
        this.quantity = quantity;
    }

    public void markAsBought(User buyer) {
        ensureNotBought();
        this.bought = true;
        this.boughtBy = buyer;
    }

    private void ensureNotBought() {
        if (bought) {
            throw DomainException.conflict("Bought entries cannot be changed");
        }
    }

    public Long getId() {
        return id;
    }

    public Home getHome() {
        return home;
    }

    public Item getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public boolean isBought() {
        return bought;
    }

    public User getBoughtBy() {
        return boughtBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
