package com.parkease.auth_service.dto.response;

import com.parkease.auth_service.enums.Role;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String vehiclePlate;
    private Boolean isActive;
    private String profilePicUrl;
    private LocalDateTime createdAt;
}