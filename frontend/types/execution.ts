export type ExecutionStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'RETRYING' | 'CANCELLED';

export interface Execution {
  id: string;
  jobId: string;
  jobName?: string;
  organizationId: string;
  sourceEventId?: string;
  status: ExecutionStatus;
  attempt: number;
  scheduledAt?: string;
  startedAt?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
  errorMessage?: string;
  workerId?: string;
  result?: string;
  durationMs?: number;
}
