package com.chronos.notification.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Default {@link NotificationProvider} implementation for the current milestone.
 *
 * <p>This provider does NOT make any network calls. It simulates successful
 * delivery by logging all notification fields and returning
 * {@link NotificationResult#success()}. It serves as the pluggable foundation
 * through which real providers (email, webhook) will be introduced later.
 *
 * <p>Marked {@code @Primary} so it is auto-selected when multiple
 * {@link NotificationProvider} beans exist in future milestones.
 */
@Primary
@Component
public class LoggingNotificationProvider implements NotificationProvider {

    private static final Logger logger = LoggerFactory.getLogger(LoggingNotificationProvider.class);

    @Override
    public NotificationResult send(NotificationRequest request) {
        logger.info(
                "[LoggingNotificationProvider] Delivering notification: " +
                        "notificationId={}, organizationId={}, executionId={}, jobId={}, " +
                        "type={}, recipient='{}', subject='{}'",
                request.getNotificationId(),
                request.getOrganizationId(),
                request.getExecutionId(),
                request.getJobId(),
                request.getType(),
                request.getRecipient(),
                request.getSubject()
        );
        logger.info(
                "[LoggingNotificationProvider] Message body: {}",
                request.getMessage()
        );
        logger.info(
                "[LoggingNotificationProvider] Delivery result: SUCCESS for notificationId={}",
                request.getNotificationId()
        );

        return NotificationResult.success();
    }
}
