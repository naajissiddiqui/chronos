import { apiClient } from '@/lib/axios';
import { Execution } from '@/types/execution';

export const executionService = {
  getAllExecutions: async (): Promise<Execution[]> => {
    const res = await apiClient.get<Execution[]>('/api/v1/executions');
    return res.data;
  },

  getExecutionById: async (id: string): Promise<Execution> => {
    const res = await apiClient.get<Execution>(`/api/v1/executions/${id}`);
    return res.data;
  },

  getExecutionsByJobId: async (jobId: string): Promise<Execution[]> => {
    const res = await apiClient.get<Execution[]>(`/api/v1/jobs/${jobId}/executions`);
    return res.data;
  },
};
