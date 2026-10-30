package com.parkease.parkinglot_service.dto.response;

import com.parkease.parkinglot_service.enums.LotStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLotResponse {

    private UUID lotId;
    private UUID managerId;
    private String name;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer totalSpots;
    private BigDecimal hourlyRate;
    private LotStatus status;
    private String rejectionReason;
    private Boolean isOpen;
    private Boolean isActive;

    // Populated only during GPS nearby search queries
    private Double distanceKm;

    private Instant createdAt;
    private Instant updatedAt;
}
