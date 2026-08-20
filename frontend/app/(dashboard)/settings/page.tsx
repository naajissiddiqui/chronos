'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { authService } from '@/services/auth.service';
import { useAuthStore } from '@/store/auth-store';
import { Key, Plus, Trash2, Shield, Loader2, Copy, Check } from 'lucide-react';
import { toast } from 'sonner';

export default function SettingsPage() {
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const [newKeyName, setNewKeyName] = useState('');
  const [createdKey, setCreatedKey] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const { data: apiKeys = [], isLoading } = useQuery({
    queryKey: ['api-keys'],
    queryFn: () => authService.listApiKeys(),
  });

  const createKeyMutation = useMutation({
    mutationFn: (name: string) => authService.createApiKey({ name }),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
      setCreatedKey(res.apiKey);
      setNewKeyName('');
      toast.success('API Key generated successfully');
    },
    onError: () => {
      toast.error('Failed to create API key');
    },
  });

  const revokeKeyMutation = useMutation({
    mutationFn: (id: string) => authService.revokeApiKey(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['api-keys'] });
      toast.success('API Key revoked');
    },
    onError: () => {
      toast.error('Failed to revoke API key');
    },
  });

  const handleCopy = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    toast.success('API Key copied to clipboard');
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="space-y-8 max-w-5xl">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Settings</h1>
        <p className="text-sm text-slate-500 mt-1">
          Manage your organization settings and API access keys.
        </p>
      </div>

      {/* Organization Overview */}
      <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-4">
        <h3 className="font-bold text-slate-900 text-sm flex items-center gap-2">
          <Shield className="w-4 h-4 text-blue-600" />
          <span>Organization Details</span>
        </h3>
        <div className="grid grid-cols-2 gap-4 text-xs">
          <div>
            <span className="text-slate-400 font-medium block">Organization ID</span>
            <span className="font-mono text-slate-800 font-semibold">{user?.organizationId || 'org_01HZQP1A'}</span>
          </div>
          <div>
            <span className="text-slate-400 font-medium block">Account Email</span>
            <span className="text-slate-800 font-semibold">{user?.email || 'ethan@acme.io'}</span>
          </div>
        </div>
      </div>

      {/* API Keys Management */}
      <div className="bg-white p-6 rounded-xl border border-slate-200/80 shadow-xs space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-bold text-slate-900 text-sm flex items-center gap-2">
              <Key className="w-4 h-4 text-blue-600" />
              <span>API Access Keys</span>
            </h3>
            <p className="text-xs text-slate-500 mt-0.5">
              API keys allow programmatic access to Chronos Job & Execution APIs.
            </p>
          </div>
        </div>

        {/* Generate Key Form */}
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (newKeyName.trim()) createKeyMutation.mutate(newKeyName.trim());
          }}
          className="flex gap-3"
        >
          <input
            type="text"
            required
            value={newKeyName}
            onChange={(e) => setNewKeyName(e.target.value)}
            placeholder="Key name (e.g. production-worker-key)"
            className="flex-1 px-3.5 py-2 text-xs bg-white border border-slate-200 rounded-lg text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 shadow-xs"
          />
          <button
            type="submit"
            disabled={createKeyMutation.isPending}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded-lg shadow-xs transition-colors flex items-center gap-2 shrink-0"
          >
            {createKeyMutation.isPending ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Plus className="w-3.5 h-3.5" />}
            <span>Generate Key</span>
          </button>
        </form>

        {/* Generated Key Modal/Banner */}
        {createdKey && (
          <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-950 space-y-2">
            <div className="text-xs font-bold text-emerald-900">Make sure to copy your API key now. You won't be able to see it again!</div>
            <div className="flex items-center gap-2 font-mono text-xs bg-white p-2.5 rounded-lg border border-emerald-200">
              <span className="flex-1 truncate">{createdKey}</span>
              <button
                onClick={() => handleCopy(createdKey)}
                className="p-1.5 hover:bg-emerald-100 rounded text-emerald-700 transition-colors"
              >
                {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
              </button>
            </div>
          </div>
        )}

        {/* API Keys Table */}
        <div className="border border-slate-200/80 rounded-lg overflow-hidden">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-200/80 bg-slate-50/50 text-slate-500 font-semibold uppercase tracking-wider">
                <th className="py-3 px-4">Name</th>
                <th className="py-3 px-4">Key Prefix</th>
                <th className="py-3 px-4">Created</th>
                <th className="py-3 px-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-700">
              {isLoading ? (
                <tr>
                  <td colSpan={4} className="py-6 text-center text-slate-400">
                    Loading API keys...
                  </td>
                </tr>
              ) : apiKeys.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-6 text-center text-slate-400">
                    No active API keys found.
                  </td>
                </tr>
              ) : (
                apiKeys.map((key) => (
                  <tr key={key.id} className="hover:bg-slate-50/80">
                    <td className="py-3 px-4 font-semibold text-slate-900">{key.name}</td>
                    <td className="py-3 px-4 font-mono text-slate-500">{key.keyPrefix || 'chr_live_...'}</td>
                    <td className="py-3 px-4 text-slate-500">
                      {key.createdAt ? new Date(key.createdAt).toLocaleDateString() : 'Just now'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => revokeKeyMutation.mutate(key.id)}
                        className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-md transition-colors"
                        title="Revoke key"
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
