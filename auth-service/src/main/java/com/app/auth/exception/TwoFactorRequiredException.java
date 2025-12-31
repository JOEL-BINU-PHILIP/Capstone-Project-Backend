package com.app.auth.exception;

public class TwoFactorRequiredException extends AuthException {
    private final String tempToken;

    public TwoFactorRequiredException(String message, String tempToken) {
        super(message);
        this.tempToken = tempToken;
    }

    public String getTempToken() {
        return tempToken;
    }
}
