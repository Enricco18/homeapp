package com.home.backend.controllers.dtos;

import com.home.backend.entities.enums.Store;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// usado no create e no update
public record ItemRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Store store
) {
}