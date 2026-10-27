package com.parkease.auth_service.service;


import com.parkease.auth_service.entity.AuditLog;
import com.parkease.auth_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public  void logAction(UUID actorId, String action , String entityName, UUID entityId ,String before , String after){
        try {
            AuditLog auditLogs = AuditLog.builder()
                    .actorId(actorId)
                    .action(action)
                    .entityId(entityId)
                    .entityName(entityName)
                    .beforeValue(before)
                    .afterValue(after)
                    .build();

            auditLogRepository.save(auditLogs);

            log.info("Audit log recorded successfully action={} , actorId={} , entityId={}",action , actorId , entityId);
        }catch (Exception exception){
            log.error("Failed to Record teh Audti log : {}",exception.getMessage() , exception);
        }
    }


    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByActor(UUID actorId){
        return auditLogRepository.findByActorId(actorId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsForEntity(String entityName , UUID entityId){
        return auditLogRepository.findByEntityNameAndEntityIdOrderByTimestampDesc(entityName,entityId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAllPaginatedLogs(Pageable pageable){
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
