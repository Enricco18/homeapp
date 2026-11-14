package com.home.backend.services;

import com.home.backend.clients.ClerkClient;
import com.home.backend.clients.ClerkUser;
import com.home.backend.entities.User;
import com.home.backend.exceptions.DomainException;
import com.home.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    // nomes dos claims configurados em "Customize session token" no Clerk
    public static final String USERNAME_CLAIM = "username";
    public static final String FULL_NAME_CLAIM = "full_name";
    public static final String EMAIL_CLAIM = "email";
    public static final String IMAGE_URL_CLAIM = "image_url";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClerkClient clerkClient;

    // chamado a cada request autenticado: garante que o usuário existe e com o perfil do token
    @Transactional
    public void syncFromJwt(Jwt jwt) {
        userRepository.upsert(
                jwt.getSubject(),
                jwt.getClaimAsString(USERNAME_CLAIM),
                jwt.getClaimAsString(FULL_NAME_CLAIM),
                jwt.getClaimAsString(EMAIL_CLAIM),
                jwt.getClaimAsString(IMAGE_URL_CLAIM)
        );
    }

    // busca o perfil no Clerk e salva/atualiza aqui (ex.: convidado que nunca abriu o app)
    @Transactional
    public User syncFromClerk(String userId) {
        ClerkUser clerkUser = clerkClient.getUser(userId)
                .orElseThrow(() -> DomainException.notFound("User not found"));

        userRepository.upsert(
                clerkUser.id(),
                clerkUser.username(),
                clerkUser.fullName(),
                clerkUser.primaryEmail(),
                clerkUser.imageUrl()
        );
        return userRepository.getReferenceById(clerkUser.id());
    }

    // cria só com o id se não existir
    @Transactional
    public User getUserById(String userId) {
        userRepository.insertIfMissing(userId);
        return userRepository.getReferenceById(userId);
    }
}