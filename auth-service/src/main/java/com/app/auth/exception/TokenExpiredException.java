package com.app.auth.exception;

public class TokenExpiredException extends AuthException {
    public TokenExpiredException(String message) {
        super(message);
    }
}