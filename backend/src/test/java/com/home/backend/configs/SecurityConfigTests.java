package com.home.backend.configs;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests {

  @Autowired MockMvc mvc;

  @MockitoBean JwtDecoder jwtDecoder;

  private void tokenWithClaims(Map<String, Object> claims) {
    Jwt.Builder builder =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user_123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60));
    claims.forEach(builder::claim);
    given(jwtDecoder.decode(anyString())).willReturn(builder.build());
  }

  @Test
  void homesWithoutTokenIsUnauthorized() throws Exception {
    mvc.perform(get("/homes")).andExpect(status().isUnauthorized());
  }

  @Test
  void homesWithValidTokenIsOk() throws Exception {
    tokenWithClaims(Map.of());
    mvc.perform(get("/homes").header("Authorization", "Bearer token")).andExpect(status().isOk());
  }

  @Test
  void adminWithoutRoleIsForbidden() throws Exception {
    tokenWithClaims(Map.of());
    mvc.perform(get("/api/admin/x").header("Authorization", "Bearer token"))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminWithAdminRoleIsAllowed() throws Exception {
    tokenWithClaims(Map.of("metadata", Map.of("role", "admin")));
    // passa pela segurança; 404 porque ainda não existe controller em /api/admin
    mvc.perform(get("/api/admin/x").header("Authorization", "Bearer token"))
        .andExpect(status().isNotFound());
  }
}
