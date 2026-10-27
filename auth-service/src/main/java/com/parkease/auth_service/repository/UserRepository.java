package com.parkease.auth_service.repository;

import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
//    Basic Authentication methods
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);


//    Finding User by its Vehicle Licenese Plate
    Optional<User> findByVehiclePlate(String vehiclePlate);

    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

//    ADMIN Action Methods
    List<User> findAllByRole(Role role);


//    Deleting User by userId
    void deleteByUserId(UUID userId);

}
