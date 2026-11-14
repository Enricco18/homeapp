package com.home.backend.entities;

import com.home.backend.entities.enums.Store;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.Locale;

// item do catálogo de uma casa
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = Item.NAME_UNIQUE_CONSTRAINT,
        columnNames = {"home_id", "normalized_name"}))
public class Item {
    public static final String NAME_UNIQUE_CONSTRAINT = "uk_item_home_normalized_name";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    // TRIM + UPPER do nome: com a unique acima, o banco garante que "Arroz" e " arroz " são o mesmo item,
    // mesmo com duas pessoas criando ao mesmo tempo
    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Home home;

    protected Item() {
    }

    public Item(Home home, String name, Store store) {
        this.home = home;
        this.store = store;
        setName(name);
    }

    public static String normalize(String name) {
        return name.trim().toUpperCase(Locale.ROOT);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setName(String name) {
        this.name = name.trim();
        this.normalizedName = normalize(name);
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Home getHome() {
        return home;
    }
}

//ltree // adjacencia //closure table // materialized path // nested set // graph database
