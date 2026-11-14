package com.home.backend.controllers;

import com.home.backend.controllers.dtos.AddQuickMessageDTO;
import com.home.backend.controllers.dtos.QuickMessageResponse;
import com.home.backend.services.QuickMessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

// mensagem rápida da casa (só uma ativa por vez: a mais recente que não expirou)
@RestController
@RequestMapping("/homes/{homeId}/quick-message")
public class QuickMessageController {
  @Autowired private QuickMessageService quickMessageService;

  // 204 se não há mensagem válida
  @GetMapping
  public ResponseEntity<QuickMessageResponse> getQuickMessage(
      @AuthenticationPrincipal Jwt jwt, @PathVariable Long homeId) {
    return quickMessageService
        .getCurrent(homeId, jwt.getSubject())
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public QuickMessageResponse sendQuickMessage(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long homeId,
      @Valid @RequestBody AddQuickMessageDTO dto) {
    return quickMessageService.send(homeId, dto.message(), jwt.getSubject());
  }
}
