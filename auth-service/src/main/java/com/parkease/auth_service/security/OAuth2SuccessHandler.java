package com.parkease.auth_service.security;

import com.parkease.auth_service.entity.RefreshToken;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.enums.Role;
import com.parkease.auth_service.repository.RefreshTokenRepository;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;


@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenDurationMs;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        User user = userRepository.findByEmail(email.toLowerCase()).orElseGet(()->{
            User newUser = User.builder()
                    .fullName(name != null ? name : "Google User")
                    .email(email.toLowerCase())
                    .passwordHash("{OAUTH2}" + UUID.randomUUID())
                    .role(Role.DRIVER)
                    .profilePicUrl(picture)
                    .isActive(true)
                    .build();

            User savedUser = userRepository.save(newUser);
            auditLogService.logAction(savedUser.getUserId(), "USER_REGISTERED", "USER", savedUser.getUserId(),
                    null, "{\"provider\":\"GOOGLE\",\"role\":\"DRIVER\"}");
            return savedUser;
        });


        String accessToken = jwtTokenProvider.generateToken(new CustomUserDetails(user));
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(RefreshToken.builder()
                        .user(user)
                        .build());

        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setRevoked(false);

        auditLogService.logAction(user.getUserId(), "USER_LOGIN", "USER", user.getUserId(),
                null, "{\"provider\":\"GOOGLE\",\"status\":\"SUCCESS\"}");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"accessToken\":\"%s\",\"refreshToken\":\"%s\",\"tokenType\":\"Bearer\",\"expiresIn\":%d,\"email\":\"%s\",\"role\":\"%s\"}",
                accessToken, refreshToken.getToken(), accessTokenExpirationMs, user.getEmail(), user.getRole().name())
        );



    }
}
