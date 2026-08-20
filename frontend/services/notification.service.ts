import { apiClient } from '@/lib/axios';
import { NotificationItem } from '@/types/notification';

export const notificationService = {
  getAllNotifications: async (): Promise<NotificationItem[]> => {
    const res = await apiClient.get<NotificationItem[]>('/api/v1/notifications');
    return res.data;
  },

  getNotificationById: async (id: string): Promise<NotificationItem> => {
    const res = await apiClient.get<NotificationItem>(`/api/v1/notifications/${id}`);
    return res.data;
  },

  getNotificationsForExecution: async (executionId: string): Promise<NotificationItem[]> => {
    const res = await apiClient.get<NotificationItem[]>(`/api/v1/executions/${executionId}/notifications`);
    return res.data;
  },
};
