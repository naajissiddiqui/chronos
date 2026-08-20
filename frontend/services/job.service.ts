import { apiClient } from '@/lib/axios';
import { Job, CreateJobRequest, UpdateJobRequest } from '@/types/job';

export const jobService = {
  getJobs: async (): Promise<Job[]> => {
    const res = await apiClient.get<Job[]>('/api/v1/jobs');
    return res.data;
  },

  getJobById: async (id: string): Promise<Job> => {
    const res = await apiClient.get<Job>(`/api/v1/jobs/${id}`);
    return res.data;
  },

  createJob: async (data: CreateJobRequest): Promise<Job> => {
    const res = await apiClient.post<Job>('/api/v1/jobs', data);
    return res.data;
  },

  updateJob: async (id: string, data: UpdateJobRequest): Promise<Job> => {
    const res = await apiClient.put<Job>(`/api/v1/jobs/${id}`, data);
    return res.data;
  },

  updateJobStatus: async (id: string, status: 'ACTIVE' | 'PAUSED'): Promise<Job> => {
    const res = await apiClient.patch<Job>(`/api/v1/jobs/${id}/status`, { status });
    return res.data;
  },

  deleteJob: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/jobs/${id}`);
  },
};
