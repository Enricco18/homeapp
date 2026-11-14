package com.home.backend.entities;

import com.home.backend.entities.enums.HomeRole;
import com.home.backend.entities.enums.Store;
import com.home.backend.exceptions.DomainException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dona dos conjuntos da casa (membros, catálogo de itens, lista de compras).
 *
 * <p>Aqui ficam só as regras do dado ("o que é válido"). Quem pode fazer o quê é decidido
 * nos services, pelo HomeUser de quem age (HomeRole / HomePermission).
 */
@Entity
public class Home {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @OneToMany(mappedBy = "home", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<HomeUser> members = new HashSet<>();

    // catálogo de itens da casa (nome único garantido no banco: unique em normalized_name)
    @OneToMany(mappedBy = "home")
    @OrderBy("name ASC")
    private List<Item> items = new ArrayList<>();

    // lista completa (inclui o histórico de comprados)
    @OneToMany(mappedBy = "home")
    @OrderBy("bought ASC, createdAt DESC")
    private List<GroceryItem> groceryList = new ArrayList<>();

    // só as linhas pendentes, sem carregar o histórico inteiro
    @OneToMany(mappedBy = "home")
    @SQLRestriction("bought = false")
    private List<GroceryItem> pendingGroceryList = new ArrayList<>();

    protected Home() {
    }

    public Home(String name) {
        this.name = name;
    }

    // ---------- casa ----------

    public void rename(String newName) {
        this.name = newName;
    }

    // ---------- membros ----------

    public HomeUser addMember(User user, HomeRole role) {
        HomeUser homeUser = new HomeUser(this, user, role);
        members.add(homeUser);
        user.getMemberships().add(homeUser);
        return homeUser;
    }

    public void removeMember(String memberId) {
        HomeUser member = members.stream()
                .filter(m -> m.getUser().getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> DomainException.notFound("Member not found"));
        if (member.getRole() == HomeRole.OWNER) {
            throw DomainException.conflict("The owner cannot leave or be removed, delete the home instead");
        }
        members.remove(member); // orphanRemoval apaga o HomeUser
    }

    public boolean isMember(String userId) {
        return members.stream().anyMatch(m -> m.getUser().getId().equals(userId));
    }

    public Set<HomeUser> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    // ---------- catálogo de itens ----------

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Item getItem(Long itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> DomainException.notFound("Item not found"));
    }

    public Item addItem(String name, Store store) {
        Item item = new Item(this, name, store);
        items.add(item);
        return item;
    }

    public Item updateItem(Long itemId, String name, Store store) {
        Item item = getItem(itemId);
        item.setName(name);
        item.setStore(store);
        return item;
    }

    // devolve o item pro service apagar; as linhas da lista saem via ON DELETE CASCADE
    public Item removeItem(Long itemId) {
        Item item = getItem(itemId);
        items.remove(item);
        pendingGroceryList.removeIf(entry -> entry.getItem() == item);
        return item;
    }

    // ---------- lista de compras ----------

    public List<GroceryItem> getGroceryList() {
        return Collections.unmodifiableList(groceryList);
    }

    // nova linha pendente (a soma numa linha existente é decidida no service, com lock)
    public void addGroceryItem(GroceryItem entry) {
        pendingGroceryList.add(entry);
        groceryList.add(entry);
    }

    // ---------- getters ----------

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
