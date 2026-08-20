export type NotificationType = 'EMAIL' | 'SLACK' | 'PAGERDUTY' | 'WEBHOOK';
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED';

export interface NotificationItem {
  id: string;
  organizationId: string;
  executionId?: string;
  jobId?: string;
  type: NotificationType;
  status: NotificationStatus;
  recipient?: string;
  subject?: string;
  message?: string;
  createdAt: string;
  sentAt?: string;
  errorMessage?: string;
}
