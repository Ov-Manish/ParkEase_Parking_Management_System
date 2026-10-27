package com.parkease.auth_service.repository;

import com.parkease.auth_service.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
//    Audit Logs for ADMIN to see the changes happened in the Application

//    Finding all the Audit entries performed by a specific actor (e.g specific admin )
    List<AuditLog> findByActorId(UUID actorId);

//    Find the Complete change of history for a specific record (e.g. entity_name= "User", entity_id = 42)
    List<AuditLog> findByEntityNameAndEntityIdOrderByTimestampDesc(String entityName , UUID entityId);

//    Find Actions By User ("e.g USER_SUSPENDED")
    List<AuditLog> findByAction(String action);

//    Paginated List of latest platform audit logs for admin UI
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
