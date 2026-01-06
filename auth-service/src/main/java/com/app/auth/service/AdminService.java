package com.app.auth.service;

import com.app.auth.model.UserRole;

public interface AdminService {

    void removeRole(String userId, UserRole role);

    void lockUser(String userId, long durationMinutes);

    void unlockUser(String userId);
}
