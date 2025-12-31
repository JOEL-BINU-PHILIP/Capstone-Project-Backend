package com.app.auth.exception;

public class EmailNotVerifiedException extends AuthException {
    public EmailNotVerifiedException(String message) {
        super(message);
    }
}