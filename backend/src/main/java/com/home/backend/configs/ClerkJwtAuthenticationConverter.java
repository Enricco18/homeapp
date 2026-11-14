package com.home.backend.configs;

import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Converte o JWT do Clerk em autenticação, lendo a role de {@code metadata.role}.
 *
 * <p>O claim {@code metadata} precisa ser adicionado no Clerk Dashboard → Sessions → Customize
 * session token: {@code {"metadata": "{{user.public_metadata}}"}}.
 */
public class ClerkJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    return new JwtAuthenticationToken(jwt, extractAuthorities(jwt), jwt.getSubject());
  }

  private List<GrantedAuthority> extractAuthorities(Jwt jwt) {
    Map<String, Object> metadata = jwt.getClaimAsMap("metadata");
    if (metadata == null || !(metadata.get("role") instanceof String role) || role.isBlank()) {
      return List.of();
    }
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
  }
}
