package com.home.backend.repositories;

import com.home.backend.entities.Item;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

// as regras do catálogo ficam na Home; aqui só persistência e lock
public interface ItemRepository extends JpaRepository<Item, Long> {

    // SELECT ... FOR UPDATE: serializa operações concorrentes sobre o mesmo item
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Item> findLockedByIdAndHomeId(Long id, Long homeId);
}
