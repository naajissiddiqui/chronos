'use client';

import React from 'react';
import { MetricCard } from '@/components/common/metric-card';
import { StatusBadge } from '@/components/common/status-badge';
import { ApiGapBanner } from '@/components/common/api-gap-banner';
import { Layers, Activity, AlertTriangle, CheckCircle } from 'lucide-react';
import { QueueInfo } from '@/types/infrastructure';

export default function QueuesPage() {
  const queues: QueueInfo[] = [
    {
      name: 'analytics',
      status: 'healthy',
      depth: 12,
      throughputPerMin: 840,
      consumers: 3,
      processedCount: '1.3M',
      failedCount: '1.0K',
    },
    {
      name: 'payments',
      status: 'healthy',
      depth: 0,
      throughputPerMin: 2100,
      consumers: 5,
      processedCount: '8.9M',
      failedCount: '134',
    },
    {
      name: 'billing',
      status: 'degraded',
      depth: 3,
      throughputPerMin: 220,
      consumers: 2,
      processedCount: '284.0K',
      failedCount: '16.3K',
    },
    {
      name: 'notifications',
      status: 'degraded',
      depth: 847,
      throughputPerMin: 120,
      consumers: 1,
      processedCount: '4.1M',
      failedCount: '89.2K',
    },
    {
      name: 'maintenance',
      status: 'healthy',
      depth: 0,
      throughputPerMin: 45,
      consumers: 2,
      processedCount: '182.0K',
      failedCount: '201',
    },
    {
      name: 'search',
      status: 'healthy',
      depth: 24,
      throughputPerMin: 380,
      consumers: 2,
      processedCount: '920.2K',
      failedCount: '2.9K',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Title */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Queues</h1>
        <p className="text-sm text-slate-500 mt-1">
          Message queue health and throughput across all queues.
        </p>
      </div>

      {/* API Gap Banner */}
      <ApiGapBanner
        featureName="Kafka Consumer Lag & Throughput"
        requiredEndpoint="GET /api/v1/queues"
        description="Kafka message topic lag and partition metrics require an administrative queue telemetry endpoint on the Gateway to stream live throughput and consumer group counts."
      />

      {/* Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        <MetricCard title="Total Queues" value="6" icon={Layers} />
        <MetricCard title="Total Depth" value="886" subtext="messages pending" icon={Activity} />
        <MetricCard title="Throughput" value="3.7K/min" icon={CheckCircle} />
        <MetricCard title="Total Failed" value="109.9K" icon={AlertTriangle} />
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-slate-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200/80 bg-slate-50/50 text-slate-500 font-semibold uppercase tracking-wider">
                <th className="py-3.5 px-6">Queue</th>
                <th className="py-3.5 px-4">Status</th>
                <th className="py-3.5 px-4">Depth</th>
                <th className="py-3.5 px-4">Throughput</th>
                <th className="py-3.5 px-4">Consumers</th>
                <th className="py-3.5 px-4">Processed</th>
                <th className="py-3.5 px-6">Failed</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {queues.map((q) => (
                <tr key={q.name} className="hover:bg-slate-50/80 transition-colors">
                  {/* QUEUE */}
                  <td className="py-4 px-6 font-mono font-medium text-slate-900">
                    {q.name}
                  </td>

                  {/* STATUS */}
                  <td className="py-4 px-4">
                    <StatusBadge status={q.status} />
                  </td>

                  {/* DEPTH */}
                  <td className="py-4 px-4 min-w-[120px]">
                    <div className="flex items-center gap-3">
                      <span className="font-bold text-slate-900 w-8">{q.depth}</span>
                      <div className="h-1.5 flex-1 bg-slate-100 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full ${
                            q.depth > 500
                              ? 'bg-rose-500'
                              : q.depth > 0
                              ? 'bg-blue-600'
                              : 'bg-slate-200'
                          }`}
                          style={{ width: `${Math.min(100, (q.depth / 900) * 100)}%` }}
                        />
                      </div>
                    </div>
                  </td>

                  {/* THROUGHPUT */}
                  <td className="py-4 px-4 font-medium text-slate-700">
                    {q.throughputPerMin >= 1000 ? `${(q.throughputPerMin / 1000).toFixed(1)}K` : q.throughputPerMin}/min
                  </td>

                  {/* CONSUMERS */}
                  <td className="py-4 px-4 text-slate-700 font-medium">{q.consumers}</td>

                  {/* PROCESSED */}
                  <td className="py-4 px-4 font-medium text-slate-700">{q.processedCount}</td>

                  {/* FAILED */}
                  <td
                    className={`py-4 px-6 font-semibold ${
                      q.failedCount.includes('K') || Number(q.failedCount) > 1000
                        ? 'text-rose-600'
                        : 'text-slate-700'
                    }`}
                  >
                    {q.failedCount}
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
