'use client';

import React from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { jobService } from '@/services/job.service';
import { executionService } from '@/services/execution.service';
import { MetricCard } from '@/components/common/metric-card';
import { StatusBadge } from '@/components/common/status-badge';
import { Briefcase, Activity, AlertCircle, Server, ArrowUpRight } from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';

export default function DashboardPage() {
  const { data: jobs = [], isLoading: isLoadingJobs } = useQuery({
    queryKey: ['jobs'],
    queryFn: () => jobService.getJobs(),
  });

  const { data: executions = [], isLoading: isLoadingExecutions } = useQuery({
    queryKey: ['executions'],
    queryFn: () => executionService.getAllExecutions(),
  });

  const totalJobs = jobs.length;
  const runningExecutions = executions.filter((e) => e.status === 'RUNNING').length;
  const failedExecutions = executions.filter((e) => e.status === 'FAILED').length;

  // Chart dataset for 24h execution volume
  const chartData = [
    { time: '00:00', success: 190, failed: 10 },
    { time: '04:00', success: 240, failed: 15 },
    { time: '08:00', success: 180, failed: 8 },
    { time: '12:00', success: 140, failed: 12 },
    { time: '16:00', success: 210, failed: 5 },
    { time: '20:00', success: 260, failed: 18 },
  ];

  // Worker health sample matching Screenshot 5
  const sampleWorkers = [
    { name: 'worker-prod-1', status: 'Healthy', cpu: 34, color: 'bg-blue-600' },
    { name: 'worker-prod-2', status: 'Healthy', cpu: 61, color: 'bg-amber-500' },
    { name: 'worker-prod-3', status: 'Degraded', cpu: 88, color: 'bg-rose-500' },
    { name: 'worker-eu-1', status: 'Healthy', cpu: 18, color: 'bg-blue-600' },
    { name: 'worker-eu-2', status: 'Offline', cpu: 0, color: 'bg-slate-300' },
    { name: 'worker-batch-1', status: 'Healthy', cpu: 22, color: 'bg-blue-600' },
  ];

  const recentExecutionsList = executions.slice(0, 6);

  return (
    <div className="space-y-8">
      {/* Title Header */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Dashboard</h1>
        <p className="text-sm text-slate-500 mt-1">Overview of your job scheduling infrastructure.</p>
      </div>

      {/* Top Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <MetricCard
          title="Total Jobs"
          value={isLoadingJobs ? '...' : totalJobs}
          subtext="across all queues"
          icon={Briefcase}
        />
        <MetricCard
          title="Running"
          value={isLoadingExecutions ? '...' : runningExecutions}
          subtext="active executions"
          trend={{ text: '↗ 2 from yesterday', type: 'positive' }}
          icon={Activity}
        />
        <MetricCard
          title="Failed (24h)"
          value={isLoadingExecutions ? '...' : failedExecutions}
          subtext="require attention"
          trend={{ text: '↗ 1 new', type: 'negative' }}
          icon={AlertCircle}
        />
        <MetricCard
          title="Workers"
          value="4/6"
          subtext="healthy"
          icon={Server}
        />
      </div>

      {/* Main Chart: Execution volume */}
      <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-semibold text-slate-900 text-base">Execution volume</h3>
            <p className="text-xs text-slate-500 mt-0.5">Last 24 hours · all queues</p>
          </div>
          <div className="flex items-center gap-4 text-xs font-medium">
            <span className="flex items-center gap-1.5 text-slate-600">
              <span className="w-2.5 h-2.5 rounded-full bg-blue-600 inline-block" />
              Success
            </span>
            <span className="flex items-center gap-1.5 text-slate-600">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-500 inline-block" />
              Failed
            </span>
          </div>
        </div>

        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="colorSuccess" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#2563EB" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#2563EB" stopOpacity={0.0} />
                </linearGradient>
                <linearGradient id="colorFailed" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#F43F5E" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#F43F5E" stopOpacity={0.0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
              <XAxis dataKey="time" axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
              <YAxis axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
              <Tooltip
                contentStyle={{ backgroundColor: '#0F172A', borderRadius: '8px', border: 'none', color: '#fff' }}
              />
              <Area type="monotone" dataKey="success" stroke="#2563EB" strokeWidth={2} fillOpacity={1} fill="url(#colorSuccess)" />
              <Area type="monotone" dataKey="failed" stroke="#F43F5E" strokeWidth={2} fillOpacity={1} fill="url(#colorFailed)" />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Bottom Grid: Recent Executions & Worker Health */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Executions */}
        <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-slate-900 text-base">Recent executions</h3>
            <Link href="/jobs" className="text-xs text-blue-600 font-semibold hover:underline flex items-center gap-0.5">
              <span>View all</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="space-y-3">
            {recentExecutionsList.length === 0 ? (
              <div className="py-8 text-center text-xs text-slate-400">
                No executions recorded yet.
              </div>
            ) : (
              recentExecutionsList.map((ex) => (
                <div key={ex.id} className="flex items-center justify-between py-2 border-b border-slate-100 last:border-0">
                  <div className="flex items-center gap-3">
                    <span className="w-2 h-2 rounded-full shrink-0"
                      style={{
                        backgroundColor:
                          ex.status === 'SUCCESS' ? '#10B981' : ex.status === 'RUNNING' ? '#3B82F6' : '#EF4444',
                      }}
                    />
                    <div>
                      <div className="text-sm font-semibold text-slate-800">{ex.jobName || `Execution ${ex.id.slice(0, 8)}`}</div>
                      <div className="text-xs text-slate-400 mt-0.5">
                        {new Date(ex.createdAt).toLocaleString()}
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className="text-xs font-mono text-slate-500">
                      {ex.workerId || 'wrk_01HZQP1A'}
                    </div>
                    <div className="text-[11px] text-slate-400 mt-0.5">{ex.durationMs ? `${(ex.durationMs / 1000).toFixed(1)}s` : '1m 02s'}</div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Worker Health */}
        <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-slate-900 text-base">Worker health</h3>
            <Link href="/workers" className="text-xs text-blue-600 font-semibold hover:underline flex items-center gap-0.5">
              <span>View all</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="space-y-3.5">
            {sampleWorkers.map((worker) => (
              <div key={worker.name} className="space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2 font-medium text-slate-800">
                    <span
                      className={`w-2 h-2 rounded-full ${
                        worker.status === 'Healthy'
                          ? 'bg-emerald-500'
                          : worker.status === 'Degraded'
                          ? 'bg-amber-500'
                          : 'bg-slate-400'
                      }`}
                    />
                    <span>{worker.name}</span>
                  </div>
                  <span className="font-mono text-slate-500 text-[11px]">{worker.cpu}% CPU</span>
                </div>
                <div className="h-1.5 w-full bg-slate-100 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${worker.color}`}
                    style={{ width: `${worker.cpu}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
