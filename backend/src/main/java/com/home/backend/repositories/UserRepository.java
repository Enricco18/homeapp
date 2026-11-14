package com.home.backend.repositories;

import com.home.backend.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, String> {

    // cria ou atualiza num comando só (sem corrida entre requests simultâneos);
    // o WHERE faz o banco só escrever quando algum campo mudou
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO users (id, username, full_name, email, image_url)
            VALUES (:id, :username, :fullName, :email, :imageUrl)
            ON CONFLICT (id) DO UPDATE SET
                username = EXCLUDED.username,
                full_name = EXCLUDED.full_name,
                email = EXCLUDED.email,
                image_url = EXCLUDED.image_url
            WHERE (users.username, users.full_name, users.email, users.image_url)
                IS DISTINCT FROM (EXCLUDED.username, EXCLUDED.full_name, EXCLUDED.email, EXCLUDED.image_url)
            """, nativeQuery = true)
    void upsert(String id, String username, String fullName, String email, String imageUrl);

    // pra convidado que ainda não logou: cria só com o id, sem mexer se já existe
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO users (id) VALUES (:id) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void insertIfMissing(String id);
}
