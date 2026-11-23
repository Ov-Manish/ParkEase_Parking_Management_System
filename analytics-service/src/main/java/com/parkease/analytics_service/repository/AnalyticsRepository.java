package com.parkease.analytics_service.repository;

import com.parkease.analytics_service.entity.OccupancyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsRepository extends JpaRepository<OccupancyLog, UUID> {

    List<OccupancyLog> findByLotIdOrderByTimestampDesc(UUID lotId);

    List<OccupancyLog> findByLotIdAndTimestampBetweenOrderByTimestampAsc(
            UUID lotId, LocalDateTime start, LocalDateTime end
    );

    @Query("SELECT AVG(o.occupancyRate) FROM OccupancyLog o WHERE o.lotId = :lotId AND o.timestamp >= :since")
    Double findAvgOccupancyRateSince(@Param("lotId") UUID lotId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(o.occupancyRate) FROM OccupancyLog o WHERE o.timestamp >= :since")
    Double findPlatformAvgOccupancy(@Param("since") LocalDateTime since);
}
