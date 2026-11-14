package com.home.backend.exceptions;

/**
 * Violação de regra de negócio, lançada pelas entidades e services.
 * Não depende de HTTP: o ApiExceptionHandler traduz o tipo pro status da resposta.
 */
public class DomainException extends RuntimeException {

    public enum Type {
        NOT_FOUND,  // 404 (também usado pra "não é membro", pra não revelar que a casa existe)
        FORBIDDEN,  // 403
        CONFLICT,   // 409
        INVALID,    // 400
        GONE        // 410
    }

    private final Type type;

    public DomainException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public static DomainException notFound(String message) {
        return new DomainException(Type.NOT_FOUND, message);
    }

    public static DomainException forbidden(String message) {
        return new DomainException(Type.FORBIDDEN, message);
    }

    public static DomainException conflict(String message) {
        return new DomainException(Type.CONFLICT, message);
    }

    public static DomainException invalid(String message) {
        return new DomainException(Type.INVALID, message);
    }

    public static DomainException gone(String message) {
        return new DomainException(Type.GONE, message);
    }
}
