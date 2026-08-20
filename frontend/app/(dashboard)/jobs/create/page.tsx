'use client';

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { jobService } from '@/services/job.service';
import { Info, Loader2 } from 'lucide-react';
import { toast } from 'sonner';

export default function CreateJobPage() {
  const router = useRouter();
  const queryClient = useQueryClient();

  const [name, setName] = useState('sync-user-analytics');
  const [description, setDescription] = useState('Describe what this job does and any important operational notes...');
  const [queue, setQueue] = useState('analytics');
  const [maxRetries, setMaxRetries] = useState(3);
  const [schedule, setSchedule] = useState('0 */6 * * *');
  const [timezone, setTimezone] = useState('UTC');
  const [defaultPayload, setDefaultPayload] = useState('{\n  \n}');
  const [timeoutMs, setTimeoutMs] = useState(300000);
  const [concurrency, setConcurrency] = useState(1);
  const [skipIfRunning, setSkipIfRunning] = useState(true);
  const [alertOnFailure, setAlertOnFailure] = useState(true);
  const [uniqueJobs, setUniqueJobs] = useState(true);

  const cronPresets = [
    { label: 'Every hour', cron: '0 * * * *' },
    { label: 'Every 6h', cron: '0 */6 * * *' },
    { label: 'Daily at 2am', cron: '0 2 * * *' },
    { label: 'Weekly Mon', cron: '0 8 * * MON' },
    { label: 'Monthly', cron: '0 0 1 * *' },
    { label: 'Every 5m', cron: '*/5 * * * *' },
  ];

  const createJobMutation = useMutation({
    mutationFn: () =>
      jobService.createJob({
        name,
        description,
        schedule,
        timezone,
        queue,
        maxRetries,
        timeoutSeconds: Math.floor(timeoutMs / 1000),
        retryBackoffSeconds: 30,
        defaultPayload,
        concurrencyLimit: concurrency,
        skipIfRunning,
        alertOnFailure,
        uniqueJobs,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      toast.success('Job created successfully');
      router.push('/jobs');
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to create job');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createJobMutation.mutate();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-8 max-w-6xl">
      {/* Breadcrumb & Header */}
      <div className="flex items-center justify-between">
        <div>
          <div className="text-xs text-slate-400 font-medium mb-1">
            Jobs &gt; <span className="text-slate-600">Create Job</span>
          </div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Create Job</h1>
          <p className="text-sm text-slate-500 mt-1">
            Define a new scheduled or event-driven background job.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => router.push('/jobs')}
            className="px-4 py-2 bg-white border border-slate-200 text-slate-700 font-semibold text-xs rounded-lg hover:bg-slate-50 shadow-xs transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={createJobMutation.isPending}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded-lg shadow-xs transition-colors flex items-center gap-2"
          >
            {createJobMutation.isPending && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            <span>Create Job</span>
          </button>
        </div>
      </div>

      {/* Main Form Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column (2 span) */}
        <div className="lg:col-span-2 space-y-6">
          {/* Basic Configuration */}
          <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-5">
            <h3 className="font-bold text-slate-900 text-sm">Basic configuration</h3>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Job name</label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="sync-user-analytics"
                className="w-full px-3.5 py-2 text-xs font-mono bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs"
              />
              <p className="text-[11px] text-slate-400">Use kebab-case. This becomes the unique identifier for this job.</p>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Description</label>
              <textarea
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                className="w-full px-3.5 py-2.5 text-xs bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs resize-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">Queue</label>
                <input
                  type="text"
                  value={queue}
                  onChange={(e) => setQueue(e.target.value)}
                  placeholder="analytics"
                  className="w-full px-3.5 py-2 text-xs bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs"
                />
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700">Max retries</label>
                <select
                  value={maxRetries}
                  onChange={(e) => setMaxRetries(Number(e.target.value))}
                  className="w-full px-3.5 py-2 text-xs bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs"
                >
                  <option value={0}>0 attempts</option>
                  <option value={1}>1 attempt</option>
                  <option value={3}>3 attempts</option>
                  <option value={5}>5 attempts</option>
                </select>
              </div>
            </div>
          </div>

          {/* Schedule */}
          <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-5">
            <h3 className="font-bold text-slate-900 text-sm">Schedule</h3>

            <div className="space-y-1.5">
              <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700">
                <span>Cron expression</span>
                <Info className="w-3.5 h-3.5 text-slate-400" />
              </div>
              <input
                type="text"
                required
                value={schedule}
                onChange={(e) => setSchedule(e.target.value)}
                placeholder="0 */6 * * *"
                className="w-full px-3.5 py-2 text-xs font-mono bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs"
              />
            </div>

            {/* Presets Grid matching Screenshot 8 */}
            <div className="grid grid-cols-3 gap-3">
              {cronPresets.map((preset) => (
                <button
                  key={preset.label}
                  type="button"
                  onClick={() => setSchedule(preset.cron)}
                  className={`p-3 text-left rounded-lg border transition-all ${
                    schedule === preset.cron
                      ? 'bg-blue-50/60 border-blue-300 ring-1 ring-blue-500/30'
                      : 'bg-white border-slate-200 hover:border-slate-300'
                  }`}
                >
                  <div className="text-xs font-semibold text-slate-800">{preset.label}</div>
                  <div className="text-[11px] font-mono text-slate-400 mt-1">{preset.cron}</div>
                </button>
              ))}
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Timezone</label>
              <input
                type="text"
                value={timezone}
                onChange={(e) => setTimezone(e.target.value)}
                placeholder="UTC"
                className="w-full px-3.5 py-2 text-xs bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs"
              />
            </div>
          </div>

          {/* Default Payload */}
          <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-5">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-slate-900 text-sm">Default payload</h3>
              <span className="px-2 py-0.5 text-[11px] bg-slate-100 text-slate-500 rounded border border-slate-200">
                Optional
              </span>
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">JSON payload</label>
              <textarea
                rows={4}
                value={defaultPayload}
                onChange={(e) => setDefaultPayload(e.target.value)}
                className="w-full px-3.5 py-2.5 text-xs font-mono bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs resize-none"
              />
            </div>
          </div>
        </div>

        {/* Right Column (1 span) */}
        <div className="space-y-6">
          {/* Execution Settings */}
          <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-5">
            <h3 className="font-bold text-slate-900 text-sm">Execution settings</h3>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-slate-700">Timeout (ms)</label>
              <input
                type="number"
                value={timeoutMs}
                onChange={(e) => setTimeoutMs(Number(e.target.value))}
                className="w-full px-3.5 py-2 text-xs font-mono bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs"
              />
              <p className="text-[11px] text-slate-400">Job is killed after this duration.</p>
            </div>

            <div className="space-y-2">
              <label className="text-xs font-semibold text-slate-700">Concurrency</label>
              <div className="grid grid-cols-3 gap-2">
                {[1, 5, 10].map((c) => (
                  <button
                    key={c}
                    type="button"
                    onClick={() => setConcurrency(c)}
                    className={`py-2 text-xs font-semibold rounded-lg border transition-all ${
                      concurrency === c
                        ? 'bg-blue-600 text-white border-blue-600 shadow-xs'
                        : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    {c}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Advanced Toggles */}
          <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-5">
            <h3 className="font-bold text-slate-900 text-sm">Advanced</h3>

            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-800">Skip if running</div>
                  <div className="text-[11px] text-slate-400">Don't start a new run if one is active</div>
                </div>
                <input
                  type="checkbox"
                  checked={skipIfRunning}
                  onChange={(e) => setSkipIfRunning(e.target.checked)}
                  className="w-4 h-4 text-blue-600 rounded border-slate-300 focus:ring-blue-500 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-800">Alert on failure</div>
                  <div className="text-[11px] text-slate-400">Send PagerDuty alert on job failure</div>
                </div>
                <input
                  type="checkbox"
                  checked={alertOnFailure}
                  onChange={(e) => setAlertOnFailure(e.target.checked)}
                  className="w-4 h-4 text-blue-600 rounded border-slate-300 focus:ring-blue-500 cursor-pointer"
                />
              </div>

              <div className="flex items-center justify-between">
                <div>
                  <div className="text-xs font-semibold text-slate-800">Unique jobs</div>
                  <div className="text-[11px] text-slate-400">Deduplicate identical payloads</div>
                </div>
                <input
                  type="checkbox"
                  checked={uniqueJobs}
                  onChange={(e) => setUniqueJobs(e.target.checked)}
                  className="w-4 h-4 text-blue-600 rounded border-slate-300 focus:ring-blue-500 cursor-pointer"
                />
              </div>
            </div>
          </div>

          {/* SDK Configuration Box */}
          <div className="p-4 rounded-xl bg-blue-50/70 border border-blue-200/80 text-blue-900 space-y-1.5">
            <div className="flex items-center gap-2 font-semibold text-xs text-blue-950">
              <Info className="w-4 h-4 text-blue-600 shrink-0" />
              <span>SDK configuration</span>
            </div>
            <p className="text-xs text-blue-800 leading-relaxed">
              This job can also be defined in code using the Chronos SDK. Changes sync automatically.
            </p>
          </div>
        </div>
      </div>
    </form>
  );
}
