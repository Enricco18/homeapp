package com.home.backend.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.home.backend.controllers.handlers.ApiExceptionHandler;
import com.home.backend.entities.Item;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ApiExceptionHandlerTests {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  private DataIntegrityViolationException violationOf(String constraint) {
    return new DataIntegrityViolationException("violation",
        new ConstraintViolationException("violation", null, constraint));
  }

  @Test
  void duplicateItemNameIs409() {
    var response = handler.handleDataIntegrityViolation(violationOf(Item.NAME_UNIQUE_CONSTRAINT));
    assertEquals(409, response.getStatusCode().value());
  }

  @Test
  void otherViolationsAre500() {
    var response = handler.handleDataIntegrityViolation(violationOf("some_other_constraint"));
    assertEquals(500, response.getStatusCode().value());

    var withoutConstraint = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("not null"));
    assertEquals(500, withoutConstraint.getStatusCode().value());
  }
}
