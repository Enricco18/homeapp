package com.home.backend.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Backend API do Clerk (autenticada com a secret key, nunca exposta ao frontend).
 * Usada pra buscar o perfil de quem ainda não fez request no app (ex.: convidado).
 */
@Component
public class ClerkClient {
    private final RestClient restClient;

    public ClerkClient(
            @Value("${clerk.api-url}") String apiUrl,
            @Value("${clerk.secret-key}") String secretKey) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .build();
    }

    // vazio se o usuário não existe no Clerk; outros erros (chave inválida, Clerk fora) sobem como exceção
    public Optional<ClerkUser> getUser(String userId) {
        return restClient.get()
                .uri("/users/{id}", userId)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND)) {
                        return Optional.empty();
                    }
                    if (response.getStatusCode().isError()) {
                        throw new IllegalStateException("Clerk API returned " + response.getStatusCode());
                    }
                    return Optional.ofNullable(response.bodyTo(ClerkUser.class));
                });
    }
}
