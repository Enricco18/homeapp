package com.home.backend.repositories;

import com.home.backend.entities.QuickMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface QuickMessageRepository extends JpaRepository<QuickMessage, Long> {
    // a mensagem válida mais recente da casa, já com o User do autor (pra mostrar nome/foto)
    @EntityGraph(attributePaths = "author.user")
    Optional<QuickMessage> findFirstByHomeIdAndExpiresAtAfterOrderByCreatedAtDesc(Long homeId, Instant now);
}
