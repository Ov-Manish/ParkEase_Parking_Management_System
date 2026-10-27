package com.parkease.auth_service.controller;

import com.parkease.auth_service.dto.request.*;
import com.parkease.auth_service.dto.response.ApiResponse;
import com.parkease.auth_service.dto.response.AuthResponse;
import com.parkease.auth_service.dto.response.UserResponse;
import com.parkease.auth_service.entity.RefreshToken;
import com.parkease.auth_service.enums.Role;
import com.parkease.auth_service.security.CustomUserDetails;
import com.parkease.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & User Management", description = "Endpoints for registration, login, token refresh, and user profile management")
public class AuthController {

    private final AuthService authService;

    // 1. Register a new Driver or Lot Manager
    @PostMapping("/register")
    @Operation(summary = "Register a new user (Driver or Lot Manager)")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request){
        UserResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 2. User Login
    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT access token + refresh token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // 3. Refresh Access Token
    @PostMapping("/refresh")
    @Operation(summary = "Exchange an existing refresh token for a new access token and rotated refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request){
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    // 4. Logout
    @PostMapping("/logout")
    @Operation(summary = "Log out user and invalidate refresh token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal CustomUserDetails userDetails){
        authService.logout(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Successfully Logged out")
                        .build());
    }

    // 5. Get Current User Profile
    @GetMapping("/profile")
    @Operation(summary = "Get current authenticated user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        UserResponse userResponse = authService.getUserById(userDetails.getUserId());
        return ResponseEntity.ok(userResponse);
    }

    // 6. Update Profile
    @PutMapping("/profile")
    @Operation(summary = "Update current authenticated user profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> updateProfile(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid@RequestBody ProfileUpdateRequest request){
        UserResponse response = authService.updateProfile(userDetails.getUserId() , request);
        return ResponseEntity.ok(response);
    }

    // 7. Change Password
    @PutMapping("/password")
    @Operation(summary = "Change password for authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @Valid @RequestBody PasswordChangeRequest request){
       authService.changePassword(userDetails.getUserId() , request);
       return ResponseEntity.ok(ApiResponse.builder()
               .success(true)
               .message("Password Changed Successfully")
               .build());
    }


    // 8. Soft Deactivate Account
    @DeleteMapping("/deactivate")
    @Operation(summary = "Deactivate current user account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse> deactivateAccount(@AuthenticationPrincipal CustomUserDetails userDetails){
        authService.deactivateAccount(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("User Account Deactivated Successfully")
                .build());
    }

    // 9. Lookup User by ID (Internal inter-service endpoint)
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user details by ID (Internal / Admin)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId){
        UserResponse response  = authService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    // 10. Admin: Toggle User Status (Suspend / Reactivate)
    @PutMapping("/admin/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Suspend or reactivate user account", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> updateUserStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID userId,
            @RequestParam boolean isActive
    ){
        UserResponse response = authService.updateUserStatus(userDetails.getUserId() , userId, isActive);
        return ResponseEntity.ok(response);
    }

    // 11. Admin: Get All Users
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get all platform users", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<UserResponse>> getAllUsers(){
        return ResponseEntity.ok(authService.getAllUsers());
    }

    // 12. Admin: Filter Users by Role
    @GetMapping("/admin/users/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Get users filtered by role", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable Role role){
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }






}
