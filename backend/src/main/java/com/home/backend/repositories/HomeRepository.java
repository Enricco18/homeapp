package com.home.backend.repositories;

import com.home.backend.entities.Home;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeRepository extends JpaRepository<Home, Long> {
}
