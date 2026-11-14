package com.home.backend.repositories;

import com.home.backend.entities.HomeUser;
import com.home.backend.entities.identifiers.HomeUserId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeUserRepository extends JpaRepository<HomeUser, HomeUserId> {

    // casas do usuário (já traz a Home junto pra evitar N+1)
    @EntityGraph(attributePaths = "home")
    List<HomeUser> findByUserId(String userId);
}