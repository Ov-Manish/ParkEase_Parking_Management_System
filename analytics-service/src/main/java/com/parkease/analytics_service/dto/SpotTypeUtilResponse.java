package com.parkease.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpotTypeUtilResponse {
    private String spotType;
    private Integer totalSpots;
    private Integer occupiedSpots;
    private Double utilizationRate;
}
