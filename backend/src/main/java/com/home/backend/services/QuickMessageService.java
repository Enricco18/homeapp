package com.home.backend.services;

import com.home.backend.controllers.dtos.QuickMessageResponse;
import com.home.backend.entities.HomeUser;
import com.home.backend.entities.QuickMessage;
import com.home.backend.entities.enums.HomePermission;
import com.home.backend.entities.identifiers.HomeUserId;
import com.home.backend.exceptions.DomainException;
import com.home.backend.repositories.HomeUserRepository;
import com.home.backend.repositories.QuickMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Acesso: busca o HomeUser de quem age (404 se não for membro) e checa a permissão do papel (403).
 * Qualquer membro (VIEW) pode ler e mandar mensagem.
 */
@Service
public class QuickMessageService {
    private static final Duration TTL = Duration.ofMinutes(20);

    @Autowired
    private QuickMessageRepository quickMessageRepository;

    @Autowired
    private HomeUserRepository homeUserRepository;

    // a mensagem válida mais recente da casa; vazio se não há nenhuma
    @Transactional(readOnly = true)
    public Optional<QuickMessageResponse> getCurrent(Long homeId, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.VIEW);

        return quickMessageRepository
                .findFirstByHomeIdAndExpiresAtAfterOrderByCreatedAtDesc(homeId, Instant.now())
                .map(QuickMessageResponse::from);
    }

    @Transactional
    public QuickMessageResponse send(Long homeId, String message, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.VIEW);

        QuickMessage quickMessage = new QuickMessage(actor, message, Instant.now().plus(TTL));
        return QuickMessageResponse.from(quickMessageRepository.saveAndFlush(quickMessage)); // flush preenche o createdAt
    }
}
