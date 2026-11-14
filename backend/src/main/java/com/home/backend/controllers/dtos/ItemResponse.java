package com.home.backend.controllers.dtos;

import com.home.backend.entities.Item;
import com.home.backend.entities.enums.Store;

public record ItemResponse(
        Long id,
        String name,
        Store store
) {
    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getName(), item.getStore());
    }
}