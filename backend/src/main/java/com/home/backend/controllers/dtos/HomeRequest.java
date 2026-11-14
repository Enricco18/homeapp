package com.home.backend.controllers.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// usado no create e no update
public record HomeRequest(
        @NotBlank @Size(max = 100) String name
) {
}
