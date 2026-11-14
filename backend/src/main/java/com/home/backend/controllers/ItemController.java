package com.home.backend.controllers;

import com.home.backend.controllers.dtos.ItemRequest;
import com.home.backend.controllers.dtos.ItemResponse;
import com.home.backend.services.ItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// catálogo de itens da casa
@RestController
@RequestMapping("/homes/{homeId}/items")
public class ItemController {
    @Autowired
    private ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse createItem(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable Long homeId,
                                   @Valid @RequestBody ItemRequest request) {
        return itemService.createItem(homeId, jwt.getSubject(), request.name(), request.store());
    }

    @GetMapping
    public List<ItemResponse> getItems(@AuthenticationPrincipal Jwt jwt, @PathVariable Long homeId) {
        return itemService.getItems(homeId, jwt.getSubject());
    }

    @GetMapping("/{itemId}")
    public ItemResponse getItem(@AuthenticationPrincipal Jwt jwt,
                                @PathVariable Long homeId,
                                @PathVariable Long itemId) {
        return itemService.getItem(homeId, itemId, jwt.getSubject());
    }

    @PutMapping("/{itemId}")
    public ItemResponse updateItem(@AuthenticationPrincipal Jwt jwt,
                                   @PathVariable Long homeId,
                                   @PathVariable Long itemId,
                                   @Valid @RequestBody ItemRequest request) {
        return itemService.updateItem(homeId, itemId, jwt.getSubject(), request.name(), request.store());
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@AuthenticationPrincipal Jwt jwt,
                           @PathVariable Long homeId,
                           @PathVariable Long itemId) {
        itemService.deleteItem(homeId, itemId, jwt.getSubject());
    }
}
