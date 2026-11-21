package com.parkease.api_gateway.security;


import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import javax.crypto.SecretKey;

@Component

public class JwtValidator {

    private static final Logger log =  LoggerFactory.getLogger(JwtValidator.class);
    private final SecretKey key;

    public JwtValidator(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String jwtSecret
    ){
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String jwtToken){
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(jwtToken);
            return true;
        }catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }

        return false;
    }

    public Claims getClaims(String jwtToken){
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwtToken).getPayload();
    }



}
