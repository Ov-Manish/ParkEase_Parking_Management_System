package com.parkease.spot_service.config;

import com.parkease.spot_service.security.JwtAuthenticationEntryPoint;
import com.parkease.spot_service.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public Swagger API docs endpoints
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Public driver spot browse & availability endpoints
                        .requestMatchers(HttpMethod.GET, "/api/v1/spots/lot/*/available").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/spots/lot/*/count").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/spots/{spotId}").permitAll()
                        // Inter-service reservation, occupancy, and release endpoints for booking-service
                        .requestMatchers(HttpMethod.POST, "/api/v1/spots/*/reserve").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/spots/*/occupy").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/spots/*/release").permitAll()
                        // Any other action (Create, Bulk Create, Update, Delete) must be authenticated
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
