import React from 'react';
import { cn } from '@/lib/utils';
import { LucideIcon } from 'lucide-react';

interface MetricCardProps {
  title: string;
  value: string | number;
  subtext?: string;
  trend?: {
    text: string;
    isUp?: boolean;
    type?: 'positive' | 'negative' | 'neutral';
  };
  icon?: LucideIcon;
  className?: string;
}

export function MetricCard({ title, value, subtext, trend, icon: Icon, className }: MetricCardProps) {
  return (
    <div
      className={cn(
        'p-5 bg-white rounded-xl border border-slate-200/80 shadow-xs flex flex-col justify-between transition-all hover:border-slate-300',
        className
      )}
    >
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-semibold tracking-wider text-slate-500 uppercase">{title}</span>
        {Icon && <Icon className="w-4 h-4 text-slate-400 stroke-[1.75]" />}
      </div>
      <div className="mt-3">
        <div className="text-3xl font-bold text-slate-900 tracking-tight">{value}</div>
        {(subtext || trend) && (
          <div className="mt-1.5 flex items-center gap-1.5 text-xs text-slate-500">
            {subtext && <span>{subtext}</span>}
            {trend && (
              <span
                className={cn(
                  'font-medium inline-flex items-center gap-0.5',
                  trend.type === 'positive' || trend.isUp === true
                    ? 'text-emerald-600'
                    : trend.type === 'negative'
                    ? 'text-rose-600'
                    : 'text-slate-600'
                )}
              >
                {trend.text}
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
