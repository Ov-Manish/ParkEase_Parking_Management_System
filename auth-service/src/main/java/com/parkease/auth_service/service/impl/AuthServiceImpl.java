package com.parkease.auth_service.service.impl;


import com.parkease.auth_service.dto.request.*;
import com.parkease.auth_service.dto.response.AuthResponse;
import com.parkease.auth_service.dto.response.UserResponse;
import com.parkease.auth_service.entity.RefreshToken;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.enums.Role;
import com.parkease.auth_service.exception.BadRequestException;
import com.parkease.auth_service.exception.ResourceNotFoundException;
import com.parkease.auth_service.exception.TokenRefreshException;
import com.parkease.auth_service.repository.RefreshTokenRepository;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.security.CustomUserDetails;
import com.parkease.auth_service.security.JwtTokenProvider;
import com.parkease.auth_service.service.AuditLogService;
import com.parkease.auth_service.service.AuthService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl  implements AuthService {

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenDurationMs;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmailIgnoreCase(request.getEmail().trim())){
            throw new BadRequestException("Email is Already Exist : " + request.getEmail());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank() && userRepository.existsByPhone(request.getPhone().trim())) {
            throw new BadRequestException("Phone is Already Exist Try New One : "+ request.getPhone());
        }

        Role userRole = request.getRole() != null ? request.getRole() : Role.DRIVER;
        if (userRole == Role.ADMIN){
            throw new BadRequestException("Self-Registration as admin is Strictly prohibited");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .role(userRole)
                .vehiclePlate(request.getVehiclePlate() != null ? request.getVehiclePlate().trim().toUpperCase() : null)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        auditLogService.logAction(
                savedUser.getUserId(),
                "USER_REGISTERED",
                "USER",
                savedUser.getUserId(),
                null,
                String.format("{\"email\":\"%s\",\"role\":\"%s\"}",savedUser.getEmail(),savedUser.getRole())
        );

        return  mapToResponse(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(),
                        request.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

//        Generating Access Token (JWT)
        String accessToken = jwtTokenProvider.generateToken(userDetails);

//        Create or Rotate Fresh Token
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(()-> new ResourceNotFoundException("User Not Found : " + userDetails.getUserId()));

        RefreshToken refreshToken = createOrUpdateRefreshToken(user);

//        Audit Log

        auditLogService.logAction(
                user.getUserId(),
                "USER_LOGIN",
                "USER",
                user.getUserId(),
                null,
                "{\"status\":\"SUCCESS\"}"
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getJwtExpirationMs())
                .user(mapToResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenString = request.getRefreshToken();

//        FIND THE TOKEN IN DB

        RefreshToken refreshToken = refreshTokenRepository.findByToken(tokenString)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not Found in databse"));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())){
//            Revoke all token for this user
            refreshTokenRepository.deleteByUser(refreshToken.getUser());
            throw  new TokenRefreshException("Refresh Token was previously revoked. Security Compromises Detected Please Log in again ");
        }

//        Checking the expiry of the token
        if (refreshToken.getExpiryDate().isBefore(Instant.now())){
            refreshTokenRepository.delete(refreshToken);

            throw new TokenRefreshException("Refresh Token has expired please login again");
        }

        User user = refreshToken.getUser();
        if (!user.isActive()){
            throw new BadRequestException("Account is suspended or deactivated");
        }

//        Rotatre Refresh Token (Issue new Token and new Refresh Token)

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAcessToken  = jwtTokenProvider.generateToken(userDetails);

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        RefreshToken rotatedToken = refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .tokenType("Bearer")
                .accessToken(newAcessToken)
                .refreshToken(rotatedToken.getToken())
                .expiresIn(jwtTokenProvider.getJwtExpirationMs())
                .user(mapToResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User not Found : "+ userId));

        refreshTokenRepository.deleteByUser(user);

        auditLogService.logAction(
                user.getUserId(),
                "USER_LOGOUT",
                "USER",
                user.getUserId(),
                null,
                "{\"status\":\"LOGGED_OUT\"}"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User Not Found : "+userId));
        return mapToResponse(user);
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(()-> new ResourceNotFoundException("User Not Found : "+email));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new ResourceNotFoundException("User Not Found : "+userId));

        String beforeValue  = String.format("{\"fullName\":\"%s\",\"phone\":\"%s\",\"vehiclePlate\":\"%s\"}",
                user.getFullName() , user.getPhone() , user.getVehiclePlate());

        if (request.getFullName() != null && !request.getFullName().isBlank()){
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()){
            user.setPhone(request.getPhone().trim());
        }
        if (request.getVehiclePlate() != null && !request.getVehiclePlate().isBlank()){
            user.setVehiclePlate(request.getVehiclePlate().trim().toUpperCase());
        }
        if (request.getProfilePicUrl() != null && !request.getProfilePicUrl().isBlank()){
            user.setProfilePicUrl(request.getProfilePicUrl().trim());
        }

        User updatedUser =  userRepository.save(user);
        String afterValue = String.format("{\"fullName\":\"%s\",\"phone\":\"%s\",\"vehiclePlate\":\"%s\"}",
                updatedUser.getFullName() , updatedUser.getPhone() , updatedUser.getVehiclePlate());

        auditLogService.logAction(userId , "PROFILE_UPDATED","USER",userId,beforeValue,afterValue);
        return mapToResponse(updatedUser);

    }

    @Override
    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Edge Case 1: Check current password matches
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())){
            throw new BadRequestException("Current Password Does Not Match ");
        }

        // Edge Case 2: New password cannot be identical to current password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())){
            throw new BadRequestException("New Password can not be same as you current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Security best practice: Revoke active refresh tokens on password change
        refreshTokenRepository.deleteByUser(user);

        auditLogService.logAction(
                user.getUserId(),
                "PASSWORD_CHANGED",
                "USER",
                userId,
                null,
                "{\"changed\":true}"
        );

    }

    @Override
    @Transactional
    public void deactivateAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setActive(false);
        userRepository.save(user);

//        Revoke active session
        refreshTokenRepository.deleteByUser(user);

        auditLogService.logAction(
                user.getUserId(),
                "ACCOUNT_DEACTIVATED",
                "USER",
                userId,
                "{\"isActive\":true}",
                "{\"isActive\":false}"
        );
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(UUID adminId, UUID targetUserId, boolean isActive) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found with id: " + targetUserId));

        boolean previousStatus = user.isActive();
        user.setActive(isActive);
        User updatedUser = userRepository.save(user);

        if (!isActive){
            refreshTokenRepository.deleteByUser(user);
        }

        String action = isActive ? "USER_REACTIVATED" : "USER_SUSPENDED";
        auditLogService.logAction(
                adminId,
                action,
                "USER",
                targetUserId,
                String.format("{\"isActive\":%b}", previousStatus),
                String.format("{\"isActive\":%b}", isActive)
        );

        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<UserResponse> getUsersByRole(Role role) {
        return userRepository.findAllByRole(role).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }





// Helper: Refresh Token creation or rotation
    private RefreshToken createOrUpdateRefreshToken(User user){
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(RefreshToken.builder()
                        .user(user)
                        .build());

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setRevoked(false);

       return refreshTokenRepository.save(refreshToken);
    }

    private UserResponse mapToResponse(User user){
        return UserResponse.builder()
                .userId(user.getUserId())
                .role(user.getRole())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profilePicUrl(user.getProfilePicUrl())
                .vehiclePlate(user.getVehiclePlate())
                .createdAt(user.getCreatedAt())
                .isActive(user.isActive())
                .build();
    }
}
