'use client';

import React, { useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth-store';
import { ArrowRight, Clock, ShieldCheck, Zap, Activity, Cpu, Code2 } from 'lucide-react';

export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated, initialize } = useAuthStore();

  useEffect(() => {
    initialize();
  }, [initialize]);

  return (
    <div className="min-h-screen bg-white text-slate-900 flex flex-col font-sans">
      {/* Header */}
      <header className="border-b border-slate-100 px-8 py-4 flex items-center justify-between max-w-7xl mx-auto w-full">
        <div className="flex items-center gap-8">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center text-white font-bold text-sm shadow-xs">
              <svg className="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z" />
              </svg>
            </div>
            <span className="font-bold text-xl text-slate-900 tracking-tight">Chronos</span>
          </div>

          <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-600">
            <a href="#features" className="hover:text-slate-900 transition-colors">Docs</a>
            <a href="#features" className="hover:text-slate-900 transition-colors">Pricing</a>
            <a href="#features" className="hover:text-slate-900 transition-colors">Changelog</a>
            <a href="#features" className="hover:text-slate-900 transition-colors">Blog</a>
          </nav>
        </div>

        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <Link
              href="/dashboard"
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium text-sm rounded-lg shadow-xs transition-colors"
            >
              Go to Dashboard
            </Link>
          ) : (
            <>
              <Link
                href="/login"
                className="px-4 py-2 text-slate-700 hover:text-slate-900 text-sm font-medium transition-colors"
              >
                Sign in
              </Link>
              <Link
                href="/register"
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-sm rounded-lg shadow-xs transition-colors"
              >
                Get started
              </Link>
            </>
          )}
        </div>
      </header>

      {/* Hero Section */}
      <section className="pt-20 pb-16 px-6 max-w-5xl mx-auto text-left w-full space-y-8">
        <div className="inline-flex items-center gap-2 px-3 py-1 bg-blue-50 text-blue-700 text-xs font-semibold rounded-full border border-blue-200">
          <span className="w-2 h-2 rounded-full bg-blue-600 animate-pulse" />
          <span>Now in general availability — v2.4.1</span>
        </div>

        <div className="space-y-4 max-w-3xl">
          <h1 className="text-5xl md:text-6xl font-extrabold text-slate-900 tracking-tight leading-[1.1]">
            Distributed job scheduling <br />
            <span className="text-slate-400">for engineering teams.</span>
          </h1>
          <p className="text-lg text-slate-600 leading-relaxed max-w-2xl">
            Schedule, monitor, and manage background jobs across your entire infrastructure. Built for teams that can't afford downtime.
          </p>
        </div>

        <div className="flex items-center gap-4 pt-2">
          <Link
            href="/register"
            className="px-5 py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-sm rounded-lg shadow-sm transition-all inline-flex items-center gap-2"
          >
            <span>Start for free</span>
            <ArrowRight className="w-4 h-4" />
          </Link>
          <Link
            href="/dashboard"
            className="px-5 py-3 bg-white hover:bg-slate-50 text-slate-700 font-semibold text-sm rounded-lg border border-slate-200 shadow-xs transition-colors"
          >
            View demo
          </Link>
        </div>

        <p className="text-xs text-slate-400 font-medium pt-1">
          No credit card required · SOC 2 Type II certified · 99.99% uptime SLA
        </p>

        {/* Code Snippet Box */}
        <div className="mt-8 rounded-xl bg-slate-950 p-6 text-slate-100 font-mono text-sm shadow-2xl border border-slate-800 space-y-2 overflow-x-auto">
          <div className="flex items-center gap-2 pb-4 mb-2 border-b border-slate-800 text-slate-500 text-xs">
            <span className="w-3 h-3 rounded-full bg-rose-500 inline-block" />
            <span className="w-3 h-3 rounded-full bg-amber-500 inline-block" />
            <span className="w-3 h-3 rounded-full bg-emerald-500 inline-block" />
            <span className="ml-2 font-mono text-slate-400">chronos.config.ts</span>
          </div>
          <div><span className="text-purple-400">import</span> &#123; <span className="text-blue-400">defineJob</span> &#125; <span className="text-purple-400">from</span> <span className="text-emerald-400">'@chronos/sdk'</span></div>
          <br />
          <div><span className="text-purple-400">export const</span> <span className="text-yellow-300">syncAnalytics</span> = <span className="text-blue-400">defineJob</span>(&#123;</div>
          <div className="pl-6"><span className="text-red-400">name</span>: <span className="text-emerald-400">'sync-user-analytics'</span>,</div>
          <div className="pl-6"><span className="text-red-400">queue</span>: <span className="text-emerald-400">'analytics'</span>,</div>
          <div className="pl-6"><span className="text-red-400">schedule</span>: <span className="text-emerald-400">'0 */6 * * *'</span>,</div>
          <div className="pl-6"><span className="text-red-400">timeout</span>: <span className="text-amber-400">600_000</span>,</div>
          <div className="pl-6"><span className="text-red-400">retries</span>: <span className="text-amber-400">3</span>,</div>
          <div className="pl-6"><span className="text-purple-400">async</span> <span className="text-blue-400">run</span>(ctx) &#123;</div>
          <div className="pl-12 text-slate-400">// Execute distributed workflow</div>
          <div className="pl-6">&#125;</div>
          <div>&#125;)</div>
        </div>
      </section>

      {/* Grid Features Section matching Screenshot 2 */}
      <section id="features" className="py-16 px-6 max-w-5xl mx-auto w-full space-y-12">
        <h2 className="text-3xl font-extrabold text-slate-900 tracking-tight">Everything your team needs</h2>

        <div className="grid md:grid-cols-3 gap-6">
          <div className="p-6 bg-slate-50/60 rounded-xl border border-slate-200/80 space-y-3">
            <Clock className="w-6 h-6 text-blue-600 stroke-[1.75]" />
            <h3 className="font-bold text-slate-900">Cron & Event Scheduling</h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              Full cron syntax with timezone support. Trigger jobs on events via webhooks or publish directly to queues.
            </p>
          </div>

          <div className="p-6 bg-slate-50/60 rounded-xl border border-slate-200/80 space-y-3">
            <Cpu className="w-6 h-6 text-blue-600 stroke-[1.75]" />
            <h3 className="font-bold text-slate-900">Distributed Workers</h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              Deploy workers anywhere — ECS, Kubernetes, bare metal. Auto-scaling, health monitoring, and graceful shutdown built in.
            </p>
          </div>

          <div className="p-6 bg-slate-50/60 rounded-xl border border-slate-200/80 space-y-3">
            <Activity className="w-6 h-6 text-blue-600 stroke-[1.75]" />
            <h3 className="font-bold text-slate-900">Real-time Observability</h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              P50/P95/P99 latency, error rates, queue depth — all in one place. PagerDuty and Slack alerting out of the box.
            </p>
          </div>

          <div className="p-6 bg-slate-50/60 rounded-xl border border-slate-200/80 space-y-3">
            <ShieldCheck className="w-6 h-6 text-blue-600 stroke-[1.75]" />
            <h3 className="font-bold text-slate-900">Enterprise Grade</h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              SOC 2 Type II, SSO via SAML/OIDC, audit logs, role-based access control, and private deployment options.
            </p>
          </div>

          <div className="p-6 bg-slate-50/60 rounded-xl border border-slate-200/80 space-y-3">
            <Zap className="w-6 h-6 text-blue-600 stroke-[1.75]" />
            <h3 className="font-bold text-slate-900">Smart Retries</h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              Configurable retry policies with exponential backoff. Dead letter queues for failed jobs with one-click retry.
            </p>
          </div>

          <div className="p-6 bg-slate-50/60 rounded-xl border border-slate-200/80 space-y-3">
            <Code2 className="w-6 h-6 text-blue-600 stroke-[1.75]" />
            <h3 className="font-bold text-slate-900">SDK & API</h3>
            <p className="text-sm text-slate-600 leading-relaxed">
              TypeScript, Python, and Go SDKs. REST API and OpenAPI spec. Deploy from CI in minutes.
            </p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="mt-auto border-t border-slate-100 py-8 text-center text-xs text-slate-400">
        © 2026 Chronos Inc. All rights reserved.
      </footer>
    </div>
  );
}
