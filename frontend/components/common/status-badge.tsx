import React from 'react';
import { cn } from '@/lib/utils';

export type BadgeStatus = 'ACTIVE' | 'Running' | 'Success' | 'Failed' | 'Paused' | 'Queued' | 'Healthy' | 'Degraded' | 'Offline';

interface StatusBadgeProps {
  status: string;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const norm = status.trim().toUpperCase();

  let dotColor = 'bg-slate-400';
  let badgeStyle = 'bg-slate-100 text-slate-700 border-slate-200';
  let label = status;

  if (norm === 'RUNNING' || norm === 'ACTIVE') {
    dotColor = 'bg-blue-500';
    badgeStyle = 'bg-blue-50 text-blue-700 border-blue-200';
    label = norm === 'RUNNING' ? 'Running' : 'Active';
  } else if (norm === 'SUCCESS' || norm === 'HEALTHY') {
    dotColor = 'bg-emerald-500';
    badgeStyle = 'bg-emerald-50 text-emerald-700 border-emerald-200';
    label = norm === 'SUCCESS' ? 'Success' : 'Healthy';
  } else if (norm === 'FAILED') {
    dotColor = 'bg-rose-500';
    badgeStyle = 'bg-rose-50 text-rose-700 border-rose-200';
    label = 'Failed';
  } else if (norm === 'DEGRADED') {
    dotColor = 'bg-amber-500';
    badgeStyle = 'bg-amber-50 text-amber-700 border-amber-200';
    label = 'Degraded';
  } else if (norm === 'QUEUED') {
    dotColor = 'bg-amber-500';
    badgeStyle = 'bg-amber-50 text-amber-800 border-amber-200';
    label = 'Queued';
  } else if (norm === 'PAUSED') {
    dotColor = 'bg-slate-400';
    badgeStyle = 'bg-slate-100 text-slate-600 border-slate-200';
    label = 'Paused';
  } else if (norm === 'OFFLINE') {
    dotColor = 'bg-slate-400';
    badgeStyle = 'bg-slate-100 text-slate-500 border-slate-200';
    label = 'Offline';
  }

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 px-2.5 py-0.5 text-xs font-medium rounded-full border',
        badgeStyle,
        className
      )}
    >
      <span className={cn('h-1.5 w-1.5 rounded-full', dotColor)} />
      {label}
    </span>
  );
}
