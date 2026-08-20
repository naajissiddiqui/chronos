'use client';

import React, { useState } from 'react';
import { ApiGapBanner } from '@/components/common/api-gap-banner';
import { RefreshCw, AlertTriangle, Eye, RotateCcw, Trash2 } from 'lucide-react';
import { DLQMessage } from '@/types/infrastructure';
import { toast } from 'sonner';

export default function DeadLetterQueuePage() {
  const [isRefreshing, setIsRefreshing] = useState(false);

  const sampleMessages: DLQMessage[] = [
    {
      id: 'dlq_1',
      jobName: 'generate-monthly-invoices',
      queue: 'billing',
      attempts: 3,
      failedAt: 'Jul 5, 2025 13:30:00',
      errorMessage: 'Stripe API timeout after 30s: Request timed out connecting to api.stripe.com',
      payload: '{"customerId":"cus_Pm8FR9xKq1","billingPeriod":"2025-06"}',
    },
    {
      id: 'dlq_2',
      jobName: 'send-digest-emails',
      queue: 'notifications',
      attempts: 3,
      failedAt: 'Jun 29, 2025 08:04:22',
      errorMessage: 'SendGrid rate limit exceeded: 429 Too Many Requests',
      payload: '{"userId":"usr_8K2mNqPx","template":"weekly-digest"}',
    },
    {
      id: 'dlq_3',
      jobName: 'generate-monthly-invoices',
      queue: 'billing',
      attempts: 3,
      failedAt: 'Jun 1, 2025 00:02:11',
      errorMessage: 'DatabaseError: deadlock detected on invoices table',
      payload: '{"customerId":"cus_Kn4pT7yMr3","billingPeriod":"2025-05"}',
    },
    {
      id: 'dlq_4',
      jobName: 'process-payment-webhooks',
      queue: 'payments',
      attempts: 3,
      failedAt: 'Jul 3, 2025 11:18:05',
      errorMessage: 'Unexpected JSON parse error: invalid character at position 0',
      payload: '{"event":"charge.failed","id":"evt_91K3mP"}',
    },
  ];

  const handleRefresh = () => {
    setIsRefreshing(true);
    setTimeout(() => setIsRefreshing(false), 600);
  };

  const handleRetry = (msg: DLQMessage) => {
    toast.info(`Retry trigger requested for message ${msg.id}. Wire POST /api/v1/dlq/${msg.id}/retry to enable.`);
  };

  const handleDelete = (msg: DLQMessage) => {
    toast.info(`Delete requested for message ${msg.id}. Wire DELETE /api/v1/dlq/${msg.id} to enable.`);
  };

  return (
    <div className="space-y-6">
      {/* Title */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Dead Letter Queue</h1>
          <p className="text-sm text-slate-500 mt-1">
            {sampleMessages.length} failed messages awaiting review
          </p>
        </div>
        <button
          onClick={handleRefresh}
          className="inline-flex items-center gap-2 px-3.5 py-2 bg-white border border-slate-200 text-slate-700 font-semibold text-xs rounded-lg hover:bg-slate-50 shadow-xs transition-colors"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* API Gap Banner */}
      <ApiGapBanner
        featureName="Dead Letter Queue Replay & Purge"
        requiredEndpoint="GET /api/v1/dlq | POST /api/v1/dlq/{id}/retry | DELETE /api/v1/dlq/{id}"
        description="DLQ messages reside in Kafka poison pill topics. Exposing DLQ REST management endpoints allows operators to inspect payloads, trigger re-execution, or purge expired messages."
      />

      {/* Retention Notice Banner matching Screenshot 11 */}
      <div className="p-4 rounded-xl bg-amber-50/80 border border-amber-200/90 text-amber-900 flex items-center gap-3 text-xs font-medium">
        <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0" />
        <span>
          <strong>{sampleMessages.length} messages</strong> in the dead letter queue. Messages are retained for 14 days before being permanently deleted.
        </span>
      </div>

      {/* Message List */}
      <div className="bg-white rounded-xl border border-slate-200/80 shadow-xs overflow-hidden divide-y divide-slate-100">
        <div className="p-4 bg-slate-50/50 flex items-center gap-3 text-xs font-medium text-slate-600 border-b border-slate-200/80">
          <input type="checkbox" className="w-4 h-4 rounded border-slate-300 text-blue-600" />
          <span>Select all</span>
        </div>

        {sampleMessages.map((msg) => (
          <div key={msg.id} className="p-6 space-y-3.5 hover:bg-slate-50/40 transition-colors">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <input type="checkbox" className="w-4 h-4 rounded border-slate-300 text-blue-600" />
                <span className="font-bold text-sm text-slate-900">{msg.jobName}</span>
                <span className="px-2 py-0.5 text-[11px] font-mono bg-slate-100 text-slate-600 rounded border border-slate-200">
                  {msg.queue}
                </span>
                <span className="px-2 py-0.5 text-[11px] font-medium bg-rose-50 text-rose-700 rounded-full border border-rose-200">
                  ⊗ {msg.attempts} attempts
                </span>
              </div>
              <span className="text-xs text-slate-400 font-mono">{msg.failedAt}</span>
            </div>

            {/* Error Message Box */}
            <div className="p-3 bg-rose-50/80 border border-rose-200/80 rounded-lg text-rose-800 text-xs font-mono">
              {msg.errorMessage}
            </div>

            {/* Payload preview & Action Buttons */}
            <div className="flex items-center justify-between pt-1">
              <code className="text-xs font-mono text-slate-500 bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200/80 truncate max-w-xl">
                {msg.payload}
              </code>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => toast.info(`Payload for ${msg.id}:\n${msg.payload}`)}
                  className="px-3 py-1.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-medium rounded-lg shadow-xs flex items-center gap-1.5 transition-colors"
                >
                  <Eye className="w-3.5 h-3.5 text-slate-500" />
                  <span>Inspect</span>
                </button>
                <button
                  onClick={() => handleRetry(msg)}
                  className="px-3 py-1.5 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-medium rounded-lg shadow-xs flex items-center gap-1.5 transition-colors"
                >
                  <RotateCcw className="w-3.5 h-3.5 text-slate-500" />
                  <span>Retry</span>
                </button>
                <button
                  onClick={() => handleDelete(msg)}
                  className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
                  title="Delete message"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
