export type WorkerStatus = 'Healthy' | 'Degraded' | 'Offline';

export interface WorkerInfo {
  id: string;
  version: string;
  status: WorkerStatus;
  region: string;
  activeJobs: number;
  maxJobs: number;
  cpuPercent: number;
  memoryPercent: number;
  uptime: string;
  lastHeartbeatAgo: string;
}

export type QueueStatus = 'healthy' | 'degraded' | 'critical';

export interface QueueInfo {
  name: string;
  status: QueueStatus;
  depth: number;
  throughputPerMin: number;
  consumers: number;
  processedCount: string;
  failedCount: string;
}

export interface DLQMessage {
  id: string;
  jobName: string;
  queue: string;
  attempts: number;
  failedAt: string;
  errorMessage: string;
  payload: string;
}
