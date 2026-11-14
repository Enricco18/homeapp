package com.home.backend.controllers.dtos;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record AddQuickMessageDTO(@NotBlank @Length(max = 20) String message) {}
