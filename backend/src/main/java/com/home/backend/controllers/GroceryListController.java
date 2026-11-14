package com.home.backend.controllers;

import com.home.backend.controllers.dtos.AddGroceryItemRequest;
import com.home.backend.controllers.dtos.GroceryItemResponse;
import com.home.backend.controllers.dtos.UpdateGroceryItemRequest;
import com.home.backend.services.GroceryListService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// lista de compras da casa
@RestController
@RequestMapping("/homes/{homeId}/grocery-list")
public class GroceryListController {
    @Autowired
    private GroceryListService groceryListService;

    // GET /homes/1/grocery-list              -> tudo (pendentes primeiro)
    // GET /homes/1/grocery-list?bought=false -> só o que falta comprar
    @GetMapping
    public List<GroceryItemResponse> getGroceryList(@AuthenticationPrincipal Jwt jwt,
                                                    @PathVariable Long homeId,
                                                    @RequestParam(required = false) Boolean bought) {
        return groceryListService.getGroceryList(homeId, bought, jwt.getSubject());
    }

    // soma na linha pendente do item, se existir
    @PostMapping
    public GroceryItemResponse addToGroceryList(@AuthenticationPrincipal Jwt jwt,
                                                @PathVariable Long homeId,
                                                @Valid @RequestBody AddGroceryItemRequest request) {
        return groceryListService.addToGroceryList(homeId, request.itemId(), request.quantity(), jwt.getSubject());
    }

    @PatchMapping("/{entryId}")
    public GroceryItemResponse updateQuantity(@AuthenticationPrincipal Jwt jwt,
                                              @PathVariable Long homeId,
                                              @PathVariable Long entryId,
                                              @Valid @RequestBody UpdateGroceryItemRequest request) {
        return groceryListService.updateQuantity(homeId, entryId, request.quantity(), jwt.getSubject());
    }

    @PostMapping("/{entryId}/buy")
    public GroceryItemResponse markAsBought(@AuthenticationPrincipal Jwt jwt,
                                            @PathVariable Long homeId,
                                            @PathVariable Long entryId) {
        return groceryListService.markAsBought(homeId, entryId, jwt.getSubject());
    }

    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntry(@AuthenticationPrincipal Jwt jwt,
                            @PathVariable Long homeId,
                            @PathVariable Long entryId) {
        groceryListService.deleteEntry(homeId, entryId, jwt.getSubject());
    }
}
