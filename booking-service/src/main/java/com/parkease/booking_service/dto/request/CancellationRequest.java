package com.parkease.booking_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 255, message = "Cancellation reason cannot exceed 255 characters")
    private String cancellationReason;
}
