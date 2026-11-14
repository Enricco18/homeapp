package com.home.backend.services;

import com.home.backend.controllers.dtos.HomeResponse;
import com.home.backend.controllers.dtos.MemberResponse;
import com.home.backend.entities.Home;
import com.home.backend.entities.HomeUser;
import com.home.backend.entities.User;
import com.home.backend.entities.enums.HomePermission;
import com.home.backend.entities.enums.HomeRole;
import com.home.backend.entities.identifiers.HomeUserId;
import com.home.backend.exceptions.DomainException;
import com.home.backend.repositories.HomeRepository;
import com.home.backend.repositories.HomeUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Acesso: busca o HomeUser de quem age (404 se não for membro) e checa a permissão do papel (403).
 * Regras do dado: ficam na Home.
 */
@Service
public class HomeService {
    @Autowired
    private HomeRepository homeRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private HomeUserRepository homeUserRepository;

    @Transactional
    public HomeResponse createHome(String name, String userId) {
        User user = userService.getUserById(userId);

        Home home = new Home(name);
        HomeUser owner = home.addMember(user, HomeRole.OWNER); // HomeUser é salvo via cascade
        homeRepository.save(home);

        return HomeResponse.from(owner);
    }

    @Transactional(readOnly = true)
    public List<HomeResponse> getHomes(String userId) {
        return homeUserRepository.findByUserId(userId)
                .stream()
                .map(HomeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HomeResponse getHome(Long homeId, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.VIEW);

        return HomeResponse.from(actor);
    }

    @Transactional
    public HomeResponse updateHome(Long homeId, String name, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.MANAGE_HOME);

        actor.getHome().rename(name);
        return HomeResponse.from(actor);
    }

    @Transactional
    public ResponseEntity<Void> deleteHome(Long homeId, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.MANAGE_HOME);

        // members saem via cascade do JPA; itens, lista e convites via ON DELETE CASCADE no banco
        homeRepository.delete(actor.getHome());
        return ResponseEntity.noContent().build();
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(Long homeId, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        actor.require(HomePermission.VIEW);

        return actor.getHome().getMembers()
                .stream()
                .map(MemberResponse::from)
                .toList();
    }

    // qualquer membro pode sair (remover a si mesmo); remover outra pessoa exige MANAGE_MEMBERS
    @Transactional
    public ResponseEntity<Void> removeMember(Long homeId, String memberId, String userId) {
        HomeUser actor = homeUserRepository.findById(new HomeUserId(homeId, userId))
                .orElseThrow(() -> DomainException.notFound("Home not found"));
        if (!memberId.equals(userId)) {
            actor.require(HomePermission.MANAGE_MEMBERS);
        }

        actor.getHome().removeMember(memberId);
        return ResponseEntity.noContent().build();
    }
}
