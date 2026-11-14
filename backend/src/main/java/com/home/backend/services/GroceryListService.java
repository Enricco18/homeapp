package com.home.backend.services;

import com.home.backend.controllers.dtos.GroceryItemResponse;
import com.home.backend.entities.GroceryItem;
import com.home.backend.entities.Home;
import com.home.backend.entities.HomeUser;
import com.home.backend.entities.Item;
import com.home.backend.entities.enums.HomePermission;
import com.home.backend.entities.identifiers.HomeUserId;
import com.home.backend.exceptions.DomainException;
import com.home.backend.repositories.GroceryItemRepository;
import com.home.backend.repositories.HomeRepository;
import com.home.backend.repositories.HomeUserRepository;
import com.home.backend.repositories.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Acesso: busca o HomeUser de quem age (404 se não for membro) e checa a permissão do papel (403).
 * Regras do dado: no GroceryItem (linha comprada não muda mais).
 *
 * <p>Locks (SELECT ... FOR UPDATE), sempre na mesma ordem pra não dar deadlock:
 *
 * <ul>
 *   <li>adicionar: Item -> linha pendente. Duas adições do mesmo item ficam em fila, então a
 *       segunda já enxerga a linha criada pela primeira e soma nela.
 *   <li>comprar / mudar quantidade / apagar: só a linha. Evita somar numa linha que outra pessoa
 *       está marcando como comprada.
 * </ul>
 */
@Service
public class GroceryListService {
  @Autowired private GroceryItemRepository groceryItemRepository;

  @Autowired private ItemRepository itemRepository;

  @Autowired private HomeRepository homeRepository;

  @Autowired private HomeUserRepository homeUserRepository;

  // bought == null -> todas as linhas (pendentes primeiro)
  @Transactional(readOnly = true)
  public List<GroceryItemResponse> getGroceryList(Long homeId, Boolean bought, String userId) {
    HomeUser actor =
        homeUserRepository
            .findById(new HomeUserId(homeId, userId))
            .orElseThrow(() -> DomainException.notFound("Home not found"));
    actor.require(HomePermission.VIEW);

    var list = actor.getHome().getGroceryList();

    if (bought != null) {
      return list.stream()
          .filter(entry -> entry.isBought() == bought)
          .map(GroceryItemResponse::from)
          .toList();
    }

    return list.stream().map(GroceryItemResponse::from).toList();
  }

  // se já existe uma linha pendente do item, soma; senão cria uma nova
  @Transactional
  public GroceryItemResponse addToGroceryList(
      Long homeId, Long itemId, int quantity, String userId) {
    HomeUser actor =
        homeUserRepository
            .findById(new HomeUserId(homeId, userId))
            .orElseThrow(() -> DomainException.notFound("Home not found"));
    actor.require(HomePermission.EDIT_LISTS);

    Item item =
        itemRepository
            .findLockedByIdAndHomeId(itemId, homeId)
            .orElseThrow(() -> DomainException.notFound("Item not found"));

    Optional<GroceryItem> existingEntry =
        groceryItemRepository.findByHomeIdAndItemIdAndBoughtFalse(homeId, itemId);

    if (existingEntry.isPresent()) {
      GroceryItem entry = existingEntry.get();
      entry.addQuantity(quantity);
      return GroceryItemResponse.from(
          groceryItemRepository.saveAndFlush(entry)); // flush atualiza o updatedAt
    }

    Home home = actor.getHome();
    GroceryItem entry = new GroceryItem(home, item, quantity);

    return GroceryItemResponse.from(
        groceryItemRepository.saveAndFlush(entry)); // flush preenche as datas
  }

  @Transactional
  public GroceryItemResponse updateQuantity(
      Long homeId, Long entryId, int quantity, String userId) {
    HomeUser actor =
        homeUserRepository
            .findById(new HomeUserId(homeId, userId))
            .orElseThrow(() -> DomainException.notFound("Home not found"));
    actor.require(HomePermission.EDIT_LISTS);

    GroceryItem entry =
        groceryItemRepository
            .findLockedByIdAndHomeId(entryId, homeId)
            .orElseThrow(() -> DomainException.notFound("Grocery entry not found"));
    entry.changeQuantity(quantity);

    return GroceryItemResponse.from(
        groceryItemRepository.saveAndFlush(entry)); // flush atualiza o updatedAt
  }

  @Transactional
  public GroceryItemResponse markAsBought(Long homeId, Long entryId, String userId) {
    HomeUser buyer =
        homeUserRepository
            .findById(new HomeUserId(homeId, userId))
            .orElseThrow(() -> DomainException.notFound("Home not found"));
    buyer.require(HomePermission.EDIT_LISTS);

    GroceryItem entry =
        groceryItemRepository
            .findLockedByIdAndHomeId(entryId, homeId)
            .orElseThrow(() -> DomainException.notFound("Grocery entry not found"));
    entry.markAsBought(buyer.getUser());

    return GroceryItemResponse.from(
        groceryItemRepository.saveAndFlush(entry)); // updatedAt = momento da compra
  }

  // pode apagar mesmo se já foi comprada
  @Transactional
  public ResponseEntity<Void> deleteEntry(Long homeId, Long entryId, String userId) {
    HomeUser actor =
        homeUserRepository
            .findById(new HomeUserId(homeId, userId))
            .orElseThrow(() -> DomainException.notFound("Home not found"));
    actor.require(HomePermission.EDIT_LISTS);

    Optional<GroceryItem> entry = groceryItemRepository.findLockedByIdAndHomeId(entryId, homeId);

    entry.ifPresent(groceryItem -> groceryItemRepository.delete(groceryItem));
    return ResponseEntity.noContent().build();
  }
}
