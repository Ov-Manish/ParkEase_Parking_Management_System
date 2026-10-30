package com.parkease.parkinglot_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ParkEase ParkingLot Service API",
                version = "1.0",
                description = "REST API for parking facility registration, admin approval, GPS nearby search, and operational management",
                contact = @Contact(
                        name = "ParkEase Engineering Team",
                        email = "support@parkease.com"
                )
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter JWT Bearer token obtained from auth-service"
)
public class OpenApiConfig {
}
