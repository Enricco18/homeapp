package com.home.backend.configs;

import com.home.backend.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Antes de cada controller: cria/atualiza o usuário logado em "users" com os dados do token.
 * O token do Clerk dura ~60s, então uma troca de username chega aqui logo no próximo token.
 */
@Component
public class UserSyncInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken auth) {
            userService.syncFromJwt(auth.getToken());
        }
        return true;
    }
}
