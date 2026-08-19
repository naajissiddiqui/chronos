package com.chronos.notification.provider;

/**
 * Immutable value object returned by a {@link NotificationProvider} after
 * an attempted delivery. Carries success/failure status and an optional
 * error message for failed deliveries.
 */
public class NotificationResult {

    private final boolean success;
    private final String errorMessage;

    private NotificationResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static NotificationResult success() {
        return new NotificationResult(true, null);
    }

    public static NotificationResult failure(String errorMessage) {
        return new NotificationResult(false, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "NotificationResult{success=" + success +
                (errorMessage != null ? ", errorMessage='" + errorMessage + '\'' : "") +
                '}';
    }
}
