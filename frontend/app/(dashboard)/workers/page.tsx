'use client';

import React, { useState } from 'react';
import { MetricCard } from '@/components/common/metric-card';
import { StatusBadge } from '@/components/common/status-badge';
import { ApiGapBanner } from '@/components/common/api-gap-banner';
import { RefreshCw, Server, CheckCircle, AlertTriangle, XCircle } from 'lucide-react';
import { WorkerInfo } from '@/types/infrastructure';

export default function WorkersPage() {
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Sample data matching Screenshot 9
  const workers: WorkerInfo[] = [
    {
      id: 'worker-prod-1',
      version: 'v2.4.1',
      status: 'Healthy',
      region: 'us-east-1',
      activeJobs: 3,
      maxJobs: 10,
      cpuPercent: 34,
      memoryPercent: 52,
      uptime: '14d 6h 22m',
      lastHeartbeatAgo: '2s ago',
    },
    {
      id: 'worker-prod-2',
      version: 'v2.4.1',
      status: 'Healthy',
      region: 'us-east-1',
      activeJobs: 5,
      maxJobs: 10,
      cpuPercent: 61,
      memoryPercent: 68,
      uptime: '14d 6h 21m',
      lastHeartbeatAgo: '3s ago',
    },
    {
      id: 'worker-prod-3',
      version: 'v2.4.0',
      status: 'Degraded',
      region: 'us-east-1',
      activeJobs: 2,
      maxJobs: 10,
      cpuPercent: 88,
      memoryPercent: 91,
      uptime: '3d 14h 05m',
      lastHeartbeatAgo: '8s ago',
    },
    {
      id: 'worker-eu-1',
      version: 'v2.4.1',
      status: 'Healthy',
      region: 'eu-west-1',
      activeJobs: 1,
      maxJobs: 8,
      cpuPercent: 18,
      memoryPercent: 41,
      uptime: '7d 2h 44m',
      lastHeartbeatAgo: '1s ago',
    },
    {
      id: 'worker-eu-2',
      version: 'v2.4.1',
      status: 'Offline',
      region: 'eu-west-1',
      activeJobs: 0,
      maxJobs: 8,
      cpuPercent: 0,
      memoryPercent: 0,
      uptime: '—',
      lastHeartbeatAgo: '14m ago',
    },
    {
      id: 'worker-batch-1',
      version: 'v2.4.1',
      status: 'Healthy',
      region: 'us-west-2',
      activeJobs: 1,
      maxJobs: 4,
      cpuPercent: 22,
      memoryPercent: 38,
      uptime: '21d 3h 18m',
      lastHeartbeatAgo: '2s ago',
    },
  ];

  const handleRefresh = () => {
    setIsRefreshing(true);
    setTimeout(() => setIsRefreshing(false), 600);
  };

  return (
    <div className="space-y-6">
      {/* Title & Top Action */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Workers</h1>
          <p className="text-sm text-slate-500 mt-1">
            4 healthy · 1 degraded · 1 offline
          </p>
        </div>
        <button
          onClick={handleRefresh}
          className="inline-flex items-center gap-2 px-3.5 py-2 bg-white border border-slate-200 text-slate-700 font-semibold text-xs rounded-lg hover:bg-slate-50 shadow-xs transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Backend API Gap Banner */}
      <ApiGapBanner
        featureName="Live Worker Telemetry"
        requiredEndpoint="GET /api/v1/workers"
        description="Worker nodes report heartbeat and CPU/Memory metrics to Redis. To display real-time cluster state, worker-service requires a REST controller exposed on the API Gateway."
      />

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <MetricCard title="Total Workers" value="6" icon={Server} />
        <MetricCard title="Healthy" value="4" icon={CheckCircle} />
        <MetricCard title="Degraded" value="1" icon={AlertTriangle} />
        <MetricCard title="Offline" value="1" icon={XCircle} />
      </div>

      {/* Worker Table */}
      <div className="bg-white rounded-xl border border-slate-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200/80 bg-slate-50/50 text-slate-500 font-semibold uppercase tracking-wider">
                <th className="py-3.5 px-6">Worker</th>
                <th className="py-3.5 px-4">Status</th>
                <th className="py-3.5 px-4">Region</th>
                <th className="py-3.5 px-4">Jobs</th>
                <th className="py-3.5 px-4">CPU</th>
                <th className="py-3.5 px-4">Memory</th>
                <th className="py-3.5 px-4">Uptime</th>
                <th className="py-3.5 px-6">Heartbeat</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {workers.map((worker) => (
                <tr key={worker.id} className="hover:bg-slate-50/80 transition-colors">
                  {/* WORKER */}
                  <td className="py-4 px-6 font-medium text-slate-900">
                    <div className="space-y-0.5">
                      <div className="font-semibold text-slate-900">{worker.id}</div>
                      <div className="text-[11px] font-mono text-slate-400">{worker.version}</div>
                    </div>
                  </td>

                  {/* STATUS */}
                  <td className="py-4 px-4">
                    <StatusBadge status={worker.status} />
                  </td>

                  {/* REGION */}
                  <td className="py-4 px-4">
                    <span className="px-2 py-0.5 text-[11px] font-mono bg-slate-100 text-slate-700 rounded border border-slate-200">
                      {worker.region}
                    </span>
                  </td>

                  {/* JOBS */}
                  <td className="py-4 px-4 font-medium text-slate-800">
                    {worker.activeJobs}
                    <span className="text-slate-400 font-normal">/{worker.maxJobs}</span>
                  </td>

                  {/* CPU */}
                  <td className="py-4 px-4 min-w-[120px]">
                    <div className="flex items-center gap-3">
                      <div className="h-1.5 flex-1 bg-slate-100 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full ${
                            worker.cpuPercent > 80
                              ? 'bg-rose-500'
                              : worker.cpuPercent > 50
                              ? 'bg-amber-500'
                              : 'bg-blue-600'
                          }`}
                          style={{ width: `${worker.cpuPercent}%` }}
                        />
                      </div>
                      <span className="font-mono text-slate-600 w-8">{worker.cpuPercent}%</span>
                    </div>
                  </td>

                  {/* MEMORY */}
                  <td className="py-4 px-4 min-w-[120px]">
                    <div className="flex items-center gap-3">
                      <div className="h-1.5 flex-1 bg-slate-100 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full ${
                            worker.memoryPercent > 80
                              ? 'bg-rose-500'
                              : 'bg-blue-600'
                          }`}
                          style={{ width: `${worker.memoryPercent}%` }}
                        />
                      </div>
                      <span className="font-mono text-slate-600 w-8">{worker.memoryPercent}%</span>
                    </div>
                  </td>

                  {/* UPTIME */}
                  <td className="py-4 px-4 text-slate-600">{worker.uptime}</td>

                  {/* HEARTBEAT */}
                  <td className="py-4 px-6 text-emerald-600 font-medium">
                    {worker.lastHeartbeatAgo}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
