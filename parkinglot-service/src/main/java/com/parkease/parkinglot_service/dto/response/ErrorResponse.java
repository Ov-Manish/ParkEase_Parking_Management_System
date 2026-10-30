package com.parkease.parkinglot_service.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private int status;
    private String message;
    private List<String> errors;
    private String path;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
