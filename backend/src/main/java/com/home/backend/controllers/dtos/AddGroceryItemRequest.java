package com.home.backend.controllers.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddGroceryItemRequest(
        @NotNull Long itemId,
        @NotNull @Positive Integer quantity
) {
}