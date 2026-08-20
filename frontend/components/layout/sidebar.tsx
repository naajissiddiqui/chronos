'use client';

import React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';
import {
  LayoutGrid,
  Briefcase,
  Plus,
  Server,
  Layers,
  AlertTriangle,
  BarChart3,
  Settings,
  HelpCircle,
  LogOut,
  User as UserIcon,
} from 'lucide-react';
import { useAuthStore } from '@/store/auth-store';

interface NavItem {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

const navSections: NavSection[] = [
  {
    title: 'OVERVIEW',
    items: [
      { name: 'Dashboard', href: '/dashboard', icon: LayoutGrid },
    ],
  },
  {
    title: 'JOBS',
    items: [
      { name: 'All Jobs', href: '/jobs', icon: Briefcase },
      { name: 'Create Job', href: '/jobs/create', icon: Plus },
    ],
  },
  {
    title: 'INFRASTRUCTURE',
    items: [
      { name: 'Workers', href: '/workers', icon: Server },
      { name: 'Queues', href: '/queues', icon: Layers },
      { name: 'Dead Letter Queue', href: '/dead-letter-queue', icon: AlertTriangle },
    ],
  },
  {
    title: 'OBSERVABILITY',
    items: [
      { name: 'Monitoring', href: '/monitoring', icon: BarChart3 },
    ],
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuthStore();

  const userEmail = user?.email || 'ethan@acme.io';
  const userName = user?.fullName || 'Ethan Cross';

  return (
    <aside className="w-64 bg-white border-r border-slate-200/90 flex flex-col justify-between h-screen sticky top-0 shrink-0 select-none">
      {/* Brand Header */}
      <div>
        <div className="h-16 px-6 flex items-center gap-3 border-b border-slate-100">
          <div className="w-7 h-7 bg-blue-600 rounded-lg flex items-center justify-center text-white shadow-xs font-bold text-sm">
            <svg
              className="w-4 h-4 text-white"
              fill="currentColor"
              viewBox="0 0 24 24"
            >
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z"/>
            </svg>
          </div>
          <span className="font-bold text-lg text-slate-900 tracking-tight">Chronos</span>
        </div>

        {/* Nav Sections */}
        <div className="px-3 py-4 space-y-6 overflow-y-auto max-h-[calc(100vh-160px)]">
          {navSections.map((section) => (
            <div key={section.title}>
              <div className="px-3 mb-2 text-[11px] font-bold text-slate-400 tracking-wider">
                {section.title}
              </div>
              <div className="space-y-0.5">
                {section.items.map((item) => {
                  const Icon = item.icon;
                  const isActive =
                    pathname === item.href ||
                    (item.href !== '/dashboard' && pathname.startsWith(item.href));
                  return (
                    <Link
                      key={item.name}
                      href={item.href}
                      className={cn(
                        'flex items-center gap-3 px-3 py-2 text-sm font-medium rounded-lg transition-colors relative',
                        isActive
                          ? 'bg-blue-50/80 text-blue-600 font-semibold'
                          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                      )}
                    >
                      {isActive && (
                        <div className="absolute left-0 top-1.5 bottom-1.5 w-1 bg-blue-600 rounded-r-full" />
                      )}
                      <Icon
                        className={cn(
                          'w-4 h-4 shrink-0 stroke-[1.75]',
                          isActive ? 'text-blue-600' : 'text-slate-400'
                        )}
                      />
                      <span>{item.name}</span>
                    </Link>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Footer / User Area */}
      <div className="p-3 border-t border-slate-100 bg-slate-50/50 space-y-1">
        <Link
          href="/settings"
          className={cn(
            'flex items-center gap-3 px-3 py-2 text-sm font-medium text-slate-600 rounded-lg hover:bg-slate-100 transition-colors',
            pathname === '/settings' && 'bg-slate-100 text-slate-900 font-semibold'
          )}
        >
          <Settings className="w-4 h-4 text-slate-400 stroke-[1.75]" />
          <span>Settings</span>
        </Link>
        <a
          href="https://github.com/naajissiddiqui/chronos"
          target="_blank"
          rel="noreferrer"
          className="flex items-center gap-3 px-3 py-2 text-sm font-medium text-slate-600 rounded-lg hover:bg-slate-100 transition-colors"
        >
          <HelpCircle className="w-4 h-4 text-slate-400 stroke-[1.75]" />
          <span>Help & Docs</span>
        </a>

        {/* User Card */}
        <div className="mt-2 pt-2 border-t border-slate-200/60 flex items-center justify-between px-2 py-1.5">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded-full bg-slate-200 text-slate-600 flex items-center justify-center shrink-0 font-medium text-xs">
              <UserIcon className="w-4 h-4 text-slate-500" />
            </div>
            <div className="min-w-0">
              <div className="text-xs font-semibold text-slate-800 truncate">{userName}</div>
              <div className="text-[11px] text-slate-500 truncate">{userEmail}</div>
            </div>
          </div>
          <button
            onClick={logout}
            title="Sign Out"
            className="p-1.5 text-slate-400 hover:text-slate-600 hover:bg-slate-200/60 rounded-md transition-colors"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </aside>
  );
}
