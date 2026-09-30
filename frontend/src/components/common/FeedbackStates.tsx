import React from 'react';
import { Loader2, AlertTriangle, Inbox } from 'lucide-react';

export const LoadingState: React.FC<{ message?: string }> = ({ message = 'Loading operational telemetry...' }) => (
  <div className="flex flex-col items-center justify-center p-12 text-slate-400">
    <Loader2 className="h-8 w-8 animate-spin text-blue-500 mb-3" />
    <span className="text-sm font-medium tracking-wide">{message}</span>
  </div>
);

export const ErrorState: React.FC<{ message: string; onRetry?: () => void }> = ({ message, onRetry }) => (
  <div className="rounded-lg border border-red-900/50 bg-red-950/20 p-6 text-center text-red-300">
    <AlertTriangle className="mx-auto h-8 w-8 text-red-400 mb-2" />
    <h3 className="text-sm font-semibold uppercase tracking-wider">Communication Fault</h3>
    <p className="mt-1 text-sm text-red-400/90">{message}</p>
    {onRetry && (
      <button
        onClick={onRetry}
        className="mt-4 inline-flex items-center px-3 py-1.5 rounded text-xs font-medium bg-red-900/60 hover:bg-red-800 text-red-100 transition-colors border border-red-700/50"
      >
        Retry Request
      </button>
    )}
  </div>
);

export const EmptyState: React.FC<{ title: string; message: string }> = ({ title, message }) => (
  <div className="rounded-lg border border-slate-800/80 bg-slate-900/40 p-8 text-center text-slate-400">
    <Inbox className="mx-auto h-8 w-8 text-slate-500 mb-2" />
    <h3 className="text-sm font-medium text-slate-300">{title}</h3>
    <p className="mt-1 text-xs text-slate-500">{message}</p>
  </div>
);
