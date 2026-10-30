package com.parkease.parkinglot_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotRejectionRequest {

    @NotBlank(message = "Rejection feedback reason is required")
    @Size(min = 5, max = 255, message = "Rejection reason must be between 5 and 255 characters")
    private String reason;
}
