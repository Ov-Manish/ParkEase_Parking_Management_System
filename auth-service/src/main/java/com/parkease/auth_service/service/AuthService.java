package com.parkease.auth_service.service;

import com.parkease.auth_service.dto.request.*;
import com.parkease.auth_service.dto.response.AuthResponse;
import com.parkease.auth_service.dto.response.UserResponse;
import com.parkease.auth_service.enums.Role;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


public interface AuthService {


    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(UUID userId);

    UserResponse getUserById(UUID userId);

    UserResponse getUserByEmail(String email);

    UserResponse updateProfile(UUID userId , ProfileUpdateRequest request);

    void changePassword(UUID userId , PasswordChangeRequest request);

    void deactivateAccount(UUID userId);

//    Admin Related Methods
    UserResponse updateUserStatus(UUID adminId, UUID targetUserId, boolean isActive);

    List<UserResponse> getAllUsers();

    List<UserResponse> getUsersByRole(Role role);
}
