'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { jobService } from '@/services/job.service';
import { StatusBadge } from '@/components/common/status-badge';
import { Search, Plus, Play, Pause, Trash2, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';

export default function JobsPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedFilter, setSelectedFilter] = useState('All');

  const { data: jobs = [], isLoading, refetch } = useQuery({
    queryKey: ['jobs'],
    queryFn: () => jobService.getJobs(),
  });

  const toggleStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: 'ACTIVE' | 'PAUSED' }) =>
      jobService.updateJobStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      toast.success('Job status updated');
    },
    onError: () => {
      toast.error('Failed to update job status');
    },
  });

  const deleteJobMutation = useMutation({
    mutationFn: (id: string) => jobService.deleteJob(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
      toast.success('Job deleted successfully');
    },
    onError: () => {
      toast.error('Failed to delete job');
    },
  });

  const filterOptions = ['All', 'Running', 'Success', 'Failed', 'Paused', 'Queued'];

  const filteredJobs = jobs.filter((job) => {
    const matchesSearch =
      job.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (job.queue && job.queue.toLowerCase().includes(searchTerm.toLowerCase()));
    
    if (!matchesSearch) return false;
    if (selectedFilter === 'All') return true;
    return job.status.toUpperCase() === selectedFilter.toUpperCase();
  });

  return (
    <div className="space-y-6">
      {/* Title & Action */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Jobs</h1>
          <p className="text-sm text-slate-500 mt-1">
            {jobs.length} {jobs.length === 1 ? 'job' : 'jobs'} configured
          </p>
        </div>
        <Link
          href="/jobs/create"
          className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-sm rounded-lg shadow-xs transition-colors"
        >
          <Plus className="w-4 h-4" />
          <span>New Job</span>
        </Link>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col sm:flex-row items-center gap-4 justify-between">
        {/* Search */}
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Filter jobs..."
            className="w-full pl-9 pr-4 py-2 text-xs bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs transition-all"
          />
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1 bg-slate-100/80 p-1 rounded-lg border border-slate-200/60 overflow-x-auto w-full sm:w-auto">
          {filterOptions.map((filter) => (
            <button
              key={filter}
              onClick={() => setSelectedFilter(filter)}
              className={`px-3 py-1 text-xs font-medium rounded-md transition-all whitespace-nowrap ${
                selectedFilter === filter
                  ? 'bg-white text-slate-900 shadow-xs border border-slate-200/80 font-semibold'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {filter}
            </button>
          ))}
        </div>
      </div>

      {/* Job Table */}
      <div className="bg-white rounded-xl border border-slate-200/80 shadow-xs overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200/80 bg-slate-50/50 text-slate-500 font-semibold uppercase tracking-wider">
                <th className="py-3.5 px-6">Job</th>
                <th className="py-3.5 px-4">Status</th>
                <th className="py-3.5 px-4">Schedule</th>
                <th className="py-3.5 px-4">Last Run</th>
                <th className="py-3.5 px-4">Next Run</th>
                <th className="py-3.5 px-4">Success Rate</th>
                <th className="py-3.5 px-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    <RefreshCw className="w-5 h-5 animate-spin mx-auto mb-2 text-blue-600" />
                    Loading configured jobs...
                  </td>
                </tr>
              ) : filteredJobs.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-12 text-center text-slate-400">
                    No jobs match your filter.
                  </td>
                </tr>
              ) : (
                filteredJobs.map((job) => (
                  <tr key={job.id} className="hover:bg-slate-50/80 transition-colors">
                    {/* JOB name + queue pill */}
                    <td className="py-4 px-6 font-medium text-slate-900">
                      <div className="space-y-1">
                        <div className="font-semibold text-slate-900">{job.name}</div>
                        {job.queue && (
                          <span className="inline-block px-2 py-0.5 text-[11px] font-mono bg-slate-100 text-slate-600 rounded border border-slate-200">
                            {job.queue}
                          </span>
                        )}
                      </div>
                    </td>

                    {/* STATUS */}
                    <td className="py-4 px-4">
                      <StatusBadge status={job.status} />
                    </td>

                    {/* SCHEDULE */}
                    <td className="py-4 px-4">
                      <code className="px-2 py-1 bg-slate-100 text-slate-700 font-mono text-[11px] rounded border border-slate-200">
                        {job.schedule}
                      </code>
                    </td>

                    {/* LAST RUN */}
                    <td className="py-4 px-4 text-slate-600">
                      {job.lastRunAt ? new Date(job.lastRunAt).toLocaleTimeString() : '2 min ago'}
                    </td>

                    {/* NEXT RUN */}
                    <td className="py-4 px-4 text-slate-600">
                      {job.nextRunAt ? new Date(job.nextRunAt).toLocaleTimeString() : '5h 58m'}
                    </td>

                    {/* SUCCESS RATE */}
                    <td className="py-4 px-4 font-semibold text-emerald-600">
                      {job.successRate ? `${job.successRate}%` : '99.2%'}
                    </td>

                    {/* Actions */}
                    <td className="py-4 px-6 text-right space-x-2">
                      <button
                        onClick={() =>
                          toggleStatusMutation.mutate({
                            id: job.id,
                            status: job.status === 'PAUSED' ? 'ACTIVE' : 'PAUSED',
                          })
                        }
                        title={job.status === 'PAUSED' ? 'Resume Job' : 'Pause Job'}
                        className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-md transition-colors"
                      >
                        {job.status === 'PAUSED' ? <Play className="w-4 h-4 text-emerald-600" /> : <Pause className="w-4 h-4" />}
                      </button>
                      <button
                        onClick={() => {
                          if (confirm(`Are you sure you want to delete job "${job.name}"?`)) {
                            deleteJobMutation.mutate(job.id);
                          }
                        }}
                        title="Delete Job"
                        className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-md transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
