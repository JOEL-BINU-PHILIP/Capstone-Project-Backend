package com.app.auth.exception;

public class InvalidTokenException extends AuthException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
