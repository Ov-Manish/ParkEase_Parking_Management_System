package com.parkease.vehicle_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse {
    private Boolean success;
    private String message;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}