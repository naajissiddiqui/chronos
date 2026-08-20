export type JobStatus = 'ACTIVE' | 'PAUSED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'QUEUED';
export type JobPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL';

export interface Job {
  id: string;
  organizationId: string;
  name: string;
  description?: string;
  status: JobStatus;
  schedule: string;
  timezone: string;
  enabled: boolean;
  priority: JobPriority;
  timeoutSeconds: number;
  maxRetries: number;
  retryBackoffSeconds: number;
  queue?: string;
  nextRunAt?: string;
  lastRunAt?: string;
  successRate?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateJobRequest {
  name: string;
  description?: string;
  schedule: string;
  timezone?: string;
  priority?: JobPriority;
  timeoutSeconds?: number;
  maxRetries?: number;
  retryBackoffSeconds?: number;
  queue?: string;
  defaultPayload?: string;
  concurrencyLimit?: number;
  skipIfRunning?: boolean;
  alertOnFailure?: boolean;
  uniqueJobs?: boolean;
}

export interface UpdateJobRequest {
  name?: string;
  description?: string;
  schedule?: string;
  timezone?: string;
  priority?: JobPriority;
  timeoutSeconds?: number;
  maxRetries?: number;
  retryBackoffSeconds?: number;
}
