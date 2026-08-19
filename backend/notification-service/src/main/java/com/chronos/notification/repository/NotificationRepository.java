package com.chronos.notification.repository;

import com.chronos.notification.entity.Notification;
import com.chronos.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByOrganizationId(UUID organizationId);

    Optional<Notification> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<Notification> findByExecutionIdAndOrganizationId(UUID executionId, UUID organizationId);

    /**
     * Application-level idempotency check before attempting insert.
     * Used as the fast-path guard; the DB unique constraint is the safety net.
     */
    boolean existsByExecutionIdAndType(UUID executionId, NotificationType type);

    Optional<Notification> findByExecutionIdAndType(UUID executionId, NotificationType type);
}
