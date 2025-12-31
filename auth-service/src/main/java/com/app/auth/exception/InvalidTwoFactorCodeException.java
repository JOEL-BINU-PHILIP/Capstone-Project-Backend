package com.app.auth.exception;

public class InvalidTwoFactorCodeException extends AuthException {
    public InvalidTwoFactorCodeException(String message) {
        super(message);
    }
}
