package com.app.auth.service;

import com.app.auth.model.User;

public interface AuthService {

    User register(User user);

    User authenticate(String username, String password);
}
