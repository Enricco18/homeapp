package com.home.backend.controllers.dtos;

import com.home.backend.entities.GroceryItem;
import com.home.backend.entities.enums.Store;

import java.time.Instant;

public record GroceryItemResponse(
        Long id,
        Long itemId,
        String itemName,
        Store store,
        int quantity,
        boolean bought,
        String boughtBy,
        Instant boughtAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static GroceryItemResponse from(GroceryItem entry) {
        return new GroceryItemResponse(
                entry.getId(),
                entry.getItem().getId(),
                entry.getItem().getName(),
                entry.getItem().getStore(),
                entry.getQuantity(),
                entry.isBought(),
                entry.getBoughtBy() != null ? entry.getBoughtBy().getId() : null,
                entry.isBought() ? entry.getUpdatedAt() : null, // a compra é a última edição
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}