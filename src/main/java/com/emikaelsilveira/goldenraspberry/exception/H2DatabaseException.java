package com.emikaelsilveira.goldenraspberry.exception;

public class H2DatabaseException extends RuntimeException {

    public H2DatabaseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
