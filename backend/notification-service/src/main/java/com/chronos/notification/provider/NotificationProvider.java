package com.chronos.notification.provider;

/**
 * Provider abstraction for notification delivery.
 *
 * <p>The business logic in {@link com.chronos.notification.service.NotificationService}
 * depends only on this interface. Adding a new delivery channel (e.g. email, webhook,
 * SMS) requires only a new implementation annotated with {@code @Component} —
 * no changes to the service layer are needed.
 *
 * <p>Known implementations:
 * <ul>
 *   <li>{@link LoggingNotificationProvider} — default, no network calls (current milestone)</li>
 *   <li>EmailNotificationProvider — future</li>
 *   <li>WebhookNotificationProvider — future</li>
 * </ul>
 */
public interface NotificationProvider {

    /**
     * Attempt to deliver a notification.
     *
     * @param request the full notification payload
     * @return a {@link NotificationResult} indicating success or failure;
     *         must never return {@code null}
     */
    NotificationResult send(NotificationRequest request);
}
