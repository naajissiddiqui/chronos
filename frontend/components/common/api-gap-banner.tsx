import React from 'react';
import { AlertTriangle, Server, Code } from 'lucide-react';

interface ApiGapBannerProps {
  featureName: string;
  requiredEndpoint: string;
  description: string;
}

export function ApiGapBanner({ featureName, requiredEndpoint, description }: ApiGapBannerProps) {
  return (
    <div className="mb-6 p-4 rounded-xl bg-amber-50/80 border border-amber-200/90 text-amber-900 shadow-xs">
      <div className="flex items-start gap-3">
        <AlertTriangle className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
        <div className="flex-1 text-sm">
          <div className="flex items-center gap-2 font-semibold text-amber-950">
            <span>Backend Integration Required: {featureName}</span>
            <span className="px-2 py-0.5 text-[11px] font-mono font-medium rounded-md bg-amber-100 text-amber-800 border border-amber-300/60">
              API Gap Identified
            </span>
          </div>
          <p className="mt-1 text-amber-800/90 leading-relaxed">{description}</p>
          <div className="mt-2.5 flex items-center gap-2 text-xs font-mono bg-white/70 px-3 py-1.5 rounded-lg border border-amber-200/80 text-amber-950 w-fit">
            <Server className="w-3.5 h-3.5 text-amber-600" />
            <span>Required REST Endpoint:</span>
            <span className="font-bold text-amber-700">{requiredEndpoint}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
