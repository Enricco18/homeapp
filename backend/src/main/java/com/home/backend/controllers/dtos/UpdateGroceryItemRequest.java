package com.home.backend.controllers.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateGroceryItemRequest(
        @NotNull @Positive Integer quantity
) {
}