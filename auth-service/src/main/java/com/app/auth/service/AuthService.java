package com.app.auth.service;

import com.app.auth.model.User;

public interface AuthService {

    User authenticate(String username, String password);

    User register(String username, String email, String password);
}

