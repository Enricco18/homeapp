package com.home.backend.services;

import com.home.backend.controllers.dtos.ItemResponse;
import com.home.backend.entities.HomeUser;
import com.home.backend.entities.Item;
import com.home.backend.entities.enums.HomePermission;
import com.home.backend.entities.enums.Store;
import com.home.backend.entities.identifiers.HomeUserId;
import com.home.backend.exceptions.DomainException;
import com.home.backend.repositories.HomeUserRepository;
import com.home.backend.repositories.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Acesso: busca o HomeUser de quem age (404 se não for membro) e checa a permissão do papel (403).
 * Regras do dado: nome único garantido no banco (unique em normalized_name, conflito vira 409).
 */
@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private HomeUserRepository homeUserRepository;

    @Transactional
    public ItemResponse createItem(Long homeId, String userId, String name, Store store) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.EDIT_LISTS);

        Item item = actor.getHome().addItem(name, store);
        return ItemResponse.from(itemRepository.saveAndFlush(item)); // flush: conflito de nome aparece aqui
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getItems(Long homeId, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.VIEW);

        return actor.getHome().getItems()
                .stream()
                .map(ItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse getItem(Long homeId, Long itemId, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.VIEW);

        return ItemResponse.from(actor.getHome().getItem(itemId));
    }

    @Transactional
    public ItemResponse updateItem(Long homeId, Long itemId, String userId, String name, Store store) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.EDIT_LISTS);

        Item item = actor.getHome().updateItem(itemId, name, store);
        return ItemResponse.from(itemRepository.saveAndFlush(item)); // flush: conflito de nome aparece aqui
    }

    // as linhas da lista que usam o item (inclusive histórico) saem via ON DELETE CASCADE
    @Transactional
    public ResponseEntity<Void> deleteItem(Long homeId, Long itemId, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.EDIT_LISTS);

        // lock: evita alguém adicionar o item na lista enquanto ele é apagado
        itemRepository.findLockedByIdAndHomeId(itemId, homeId)
                .orElseThrow(() -> DomainException.notFound("Item not found"));

        itemRepository.delete(actor.getHome().removeItem(itemId));
        return ResponseEntity.noContent().build();
    }
}
