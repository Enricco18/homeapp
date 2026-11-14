package com.home.backend.repositories;

import com.home.backend.entities.GroceryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, Long> {

    // filtro por bought direto no banco (o histórico de comprados pode ser grande)
    @EntityGraph(attributePaths = "item")
    List<GroceryItem> findByHomeIdAndBoughtOrderByCreatedAtDesc(Long homeId, boolean bought);

    // a linha pendente do item (no máximo uma), travada pra update
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GroceryItem> findByHomeIdAndItemIdAndBoughtFalse(Long homeId, Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GroceryItem> findLockedByIdAndHomeId(Long id, Long homeId);
}
