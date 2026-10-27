package com.parkease.auth_service.repository;

import com.parkease.auth_service.entity.RefreshToken;
import com.parkease.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

//    Basic Methods

    Optional<RefreshToken> findByToken(String token);

//    Find Existing Token for specific User Method
    Optional<RefreshToken> findByUser(User user);

//    Delete or Revoke Token When user Logs Out
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user=:user")
    int deleteByUser(User user);
}
