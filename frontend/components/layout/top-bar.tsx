'use client';

import React, { useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { Search, Bell, X, CheckCircle, AlertTriangle, Plus } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { notificationService } from '@/services/notification.service';
import Link from 'next/link';

interface TopBarProps {
  title?: string;
  action?: React.ReactNode;
}

export function TopBar({ title, action }: TopBarProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [showNotifications, setShowNotifications] = useState(false);

  // Derive page title from route if not provided
  const derivedTitle =
    title ||
    (pathname === '/dashboard'
      ? 'Dashboard'
      : pathname === '/jobs'
      ? 'Jobs'
      : pathname === '/jobs/create'
      ? 'Create Job'
      : pathname === '/workers'
      ? 'Workers'
      : pathname === '/queues'
      ? 'Queues'
      : pathname === '/dead-letter-queue'
      ? 'Dead Letter Queue'
      : pathname === '/monitoring'
      ? 'Monitoring'
      : pathname === '/settings'
      ? 'Settings'
      : 'Chronos');

  const { data: notifications = [] } = useQuery({
    queryKey: ['notifications'],
    queryFn: () => notificationService.getAllNotifications(),
    enabled: showNotifications,
  });

  return (
    <header className="h-16 bg-white border-b border-slate-200/90 px-8 flex items-center justify-between sticky top-0 z-30 shrink-0">
      {/* Title / Breadcrumb */}
      <div className="flex items-center gap-2">
        <h1 className="text-base font-semibold text-slate-800">{derivedTitle}</h1>
      </div>

      {/* Right controls */}
      <div className="flex items-center gap-3">
        {/* Quick Search */}
        <div className="relative hidden md:block">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search"
            onClick={() => router.push('/jobs')}
            className="pl-9 pr-8 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg text-slate-700 focus:outline-none focus:ring-1 focus:ring-blue-500 focus:bg-white w-48 transition-all"
          />
          <kbd className="absolute right-2 top-1/2 -translate-y-1/2 px-1.5 py-0.5 text-[10px] font-mono text-slate-400 bg-white rounded border border-slate-200">
            ⌘K
          </kbd>
        </div>

        {/* Notifications Popover Toggle */}
        <div className="relative">
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            className="p-2 text-slate-500 hover:text-slate-700 hover:bg-slate-100 rounded-lg relative transition-colors"
            title="Notifications"
          >
            <Bell className="w-4 h-4 stroke-[1.75]" />
            {notifications.length > 0 && (
              <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full ring-2 ring-white" />
            )}
          </button>

          {/* Notifications Drawer */}
          {showNotifications && (
            <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-lg border border-slate-200 p-4 z-50 animate-in fade-in slide-in-from-top-2 duration-150">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <span className="font-semibold text-xs text-slate-800 uppercase tracking-wider">
                  Notifications
                </span>
                <button
                  onClick={() => setShowNotifications(false)}
                  className="text-slate-400 hover:text-slate-600"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              <div className="mt-3 space-y-2 max-h-64 overflow-y-auto">
                {notifications.length === 0 ? (
                  <div className="py-6 text-center text-xs text-slate-400">
                    No recent notifications
                  </div>
                ) : (
                  notifications.map((n) => (
                    <div
                      key={n.id}
                      className="p-2.5 rounded-lg bg-slate-50 border border-slate-100 text-xs space-y-1"
                    >
                      <div className="flex items-center gap-1.5 font-medium text-slate-800">
                        {n.status === 'SENT' ? (
                          <CheckCircle className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
                        ) : (
                          <AlertTriangle className="w-3.5 h-3.5 text-rose-500 shrink-0" />
                        )}
                        <span className="truncate">{n.subject || 'System Notification'}</span>
                      </div>
                      <p className="text-slate-600 line-clamp-2">{n.message}</p>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* Dynamic Action Button or Custom Action */}
        {action ? (
          action
        ) : (
          pathname !== '/jobs/create' && (
            <Link
              href="/jobs/create"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 text-white text-xs font-semibold rounded-lg shadow-xs hover:bg-blue-700 transition-colors"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>New Job</span>
            </Link>
          )
        )}
      </div>
    </header>
  );
}
