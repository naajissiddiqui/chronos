'use client';

import React, { useState } from 'react';
import { MetricCard } from '@/components/common/metric-card';
import { StatusBadge } from '@/components/common/status-badge';
import { ApiGapBanner } from '@/components/common/api-gap-banner';
import { RefreshCw, Activity, AlertCircle, Clock, Zap } from 'lucide-react';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

export default function MonitoringPage() {
  const [timeRange, setTimeRange] = useState('24h');
  const [isRefreshing, setIsRefreshing] = useState(false);

  const timeRanges = ['1h', '6h', '24h', '7d', '30d'];

  // Throughput chart data
  const throughputData = [
    { time: '00:00', value: 1600 },
    { time: '04:00', value: 2100 },
    { time: '08:00', value: 1400 },
    { time: '12:00', value: 1100 },
    { time: '16:00', value: 1800 },
    { time: '20:00', value: 2200 },
  ];

  // Latency percentiles data
  const latencyData = [
    { time: '00:00', p50: 120, p95: 380, p99: 810 },
    { time: '04:00', p50: 125, p95: 390, p99: 830 },
    { time: '08:00', p50: 118, p95: 375, p99: 800 },
    { time: '12:00', p50: 130, p95: 410, p99: 860 },
    { time: '16:00', p50: 124, p95: 392, p99: 831 },
    { time: '20:00', p50: 122, p95: 385, p99: 820 },
  ];

  // Error rate data
  const errorRateData = [
    { time: '00:00', rate: 1.2 },
    { time: '04:00', rate: 1.6 },
    { time: '08:00', rate: 0.9 },
    { time: '12:00', rate: 1.4 },
    { time: '16:00', rate: 0.8 },
    { time: '20:00', rate: 0.7 },
  ];

  // Queue depth bar chart data
  const queueDepthData = [
    { time: '12:00', depth: 60 },
    { time: '14:00', depth: 100 },
    { time: '16:00', depth: 145 },
    { time: '18:00', depth: 70 },
    { time: '20:00', depth: 75 },
    { time: '22:00', depth: 130 },
  ];

  const handleRefresh = () => {
    setIsRefreshing(true);
    setTimeout(() => setIsRefreshing(false), 600);
  };

  return (
    <div className="space-y-8">
      {/* Title & Controls */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Monitoring</h1>
          <p className="text-sm text-slate-500 mt-1">
            Real-time job execution metrics and system health.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* Time range selector */}
          <div className="flex items-center gap-1 bg-slate-100/80 p-1 rounded-lg border border-slate-200/60">
            {timeRanges.map((range) => (
              <button
                key={range}
                onClick={() => setTimeRange(range)}
                className={`px-2.5 py-1 text-xs font-semibold rounded-md transition-all ${
                  timeRange === range
                    ? 'bg-white text-slate-900 shadow-xs border border-slate-200/80'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {range}
              </button>
            ))}
          </div>

          <button
            onClick={handleRefresh}
            className="inline-flex items-center gap-2 px-3.5 py-1.5 bg-white border border-slate-200 text-slate-700 font-semibold text-xs rounded-lg hover:bg-slate-50 shadow-xs transition-colors"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* API Gap Banner */}
      <ApiGapBanner
        featureName="Prometheus Metrics Aggregation"
        requiredEndpoint="GET /api/v1/metrics"
        description="Spring Boot Actuator exports Prometheus metrics at `/actuator/prometheus`. A Gateway metrics aggregation service is required to stream real-time latency percentiles and error-rate time series."
      />

      {/* Metric Cards (5) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-5 gap-4">
        <MetricCard title="Throughput" value="1,680/min" trend={{ text: '↗ 12%', type: 'positive' }} icon={Zap} />
        <MetricCard title="Error Rate" value="0.8%" trend={{ text: '↘ 0.2%', type: 'positive' }} icon={AlertCircle} />
        <MetricCard title="P50 Latency" value="124ms" icon={Clock} />
        <MetricCard title="P95 Latency" value="392ms" trend={{ text: '↗ 18ms', type: 'negative' }} icon={Clock} />
        <MetricCard title="P99 Latency" value="831ms" icon={Clock} />
      </div>

      {/* Execution Throughput Chart matching Screenshot 12 */}
      <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-semibold text-slate-900 text-base">Execution throughput</h3>
            <p className="text-xs text-slate-500 mt-0.5">Jobs completed per minute · last 24h</p>
          </div>
          <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 text-xs font-semibold bg-blue-50 text-blue-700 rounded-full border border-blue-200">
            <span className="w-2 h-2 rounded-full bg-blue-600 animate-pulse" />
            Live
          </span>
        </div>

        <div className="h-60 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={throughputData}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
              <XAxis dataKey="time" axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
              <YAxis axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
              <Tooltip contentStyle={{ backgroundColor: '#0F172A', borderRadius: '8px', border: 'none', color: '#fff' }} />
              <Line type="monotone" dataKey="value" stroke="#2563EB" strokeWidth={2.5} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Side-by-Side Charts: Latency & Error Rate */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Latency percentiles */}
        <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-slate-900 text-base">Latency percentiles</h3>
            <div className="flex items-center gap-3 text-xs font-mono">
              <span className="text-blue-600 font-semibold">— P50</span>
              <span className="text-amber-500 font-semibold">— P95</span>
              <span className="text-rose-500 font-semibold">— P99</span>
            </div>
          </div>

          <div className="h-52 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={latencyData}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                <XAxis dataKey="time" axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
                <Tooltip contentStyle={{ backgroundColor: '#0F172A', borderRadius: '8px', border: 'none', color: '#fff' }} />
                <Line type="monotone" dataKey="p50" stroke="#2563EB" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="p95" stroke="#F59E0B" strokeWidth={2} dot={false} />
                <Line type="monotone" dataKey="p99" stroke="#F43F5E" strokeWidth={2} dot={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Error rate */}
        <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-slate-900 text-base">Error rate</h3>
            <span className="text-xs text-slate-400 font-medium">% of executions</span>
          </div>

          <div className="h-52 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={errorRateData}>
                <defs>
                  <linearGradient id="colorErr" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#F43F5E" stopOpacity={0.2} />
                    <stop offset="95%" stopColor="#F43F5E" stopOpacity={0.0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
                <XAxis dataKey="time" axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
                <Tooltip contentStyle={{ backgroundColor: '#0F172A', borderRadius: '8px', border: 'none', color: '#fff' }} />
                <Area type="monotone" dataKey="rate" stroke="#F43F5E" strokeWidth={2} fillOpacity={1} fill="url(#colorErr)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Queue Depth Bar Chart matching Screenshot 13 */}
      <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-semibold text-slate-900 text-base">Queue depth</h3>
            <p className="text-xs text-slate-500 mt-0.5">Messages pending across all queues</p>
          </div>
        </div>

        <div className="h-56 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={queueDepthData}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#F1F5F9" />
              <XAxis dataKey="time" axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
              <YAxis axisLine={false} tickLine={false} tick={{ fill: '#94A3B8', fontSize: 12 }} />
              <Tooltip contentStyle={{ backgroundColor: '#0F172A', borderRadius: '8px', border: 'none', color: '#fff' }} />
              <Bar dataKey="depth" fill="#3B82F6" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
