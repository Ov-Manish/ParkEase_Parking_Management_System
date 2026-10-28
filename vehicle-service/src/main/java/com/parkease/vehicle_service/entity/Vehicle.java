package com.parkease.vehicle_service.entity;

import com.parkease.vehicle_service.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicles", indexes = {
        @Index(name = "idx_vehicle_owner", columnList = "ownerId"),
        @Index(name = "idx_vehicle_plate", columnList = "licensePlate"),
        @Index(name = "idx_owner_plate", columnList = "ownerId, licensePlate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vehicleId;

    @Column(nullable = false)
    private UUID ownerId; // Driver's userId from auth-service

    @Column(nullable = false, length = 30)
    private String licensePlate; // Standardized & uppercase (e.g. "DL01AB1234")

    @Column(nullable = false, length = 50)
    private String make; // e.g. "Honda", "Tesla", "Toyota"

    @Column(nullable = false, length = 50)
    private String model; // e.g. "Civic", "Model 3", "Camry"

    @Column(length = 30)
    private String color; // e.g. "White", "Black"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VehicleType vehicleType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isEV = false; // Electric Vehicle Flag

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true; // Soft-delete flag

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime registeredAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}