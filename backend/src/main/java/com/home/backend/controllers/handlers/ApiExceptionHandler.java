package com.home.backend.controllers.handlers;

import com.home.backend.entities.Item;
import com.home.backend.exceptions.DomainException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// traduz as exceções de domínio e de banco pra respostas HTTP
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex) {
        HttpStatus status = switch (ex.getType()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INVALID -> HttpStatus.BAD_REQUEST;
            case GONE -> HttpStatus.GONE;
        };
        return problem(status, ex.getMessage());
    }

    // 409 só pro conflito esperado (dois itens com o mesmo nome criados ao mesmo tempo).
    // Qualquer outra violação é bug (schema, FK, not null...) e vira 500 com log, em vez de se passar por conflito.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        if (Item.NAME_UNIQUE_CONSTRAINT.equalsIgnoreCase(violatedConstraint(ex))) {
            return problem(HttpStatus.CONFLICT, "An item with this name already exists in this home");
        }
        log.error("Unexpected data integrity violation", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    // procura na cadeia de causas o nome da constraint que o banco reportou
    static String violatedConstraint(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
