import { useState } from 'react';
import { X, FileText, Archive } from 'lucide-react';
import apiClient from '../api/client';

interface ExportModalProps {
  contextId: string;
  contextName: string;
  onClose: () => void;
}

function triggerDownload(data: unknown, filename: string) {
  const json = JSON.stringify(data, null, 2);
  const blob = new Blob([json], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function sanitizeFilename(name: string): string {
  return name.replace(/[^a-zA-Z0-9а-яА-ЯёЁ_-]/g, '_').replace(/_+/g, '_');
}

export default function ExportModal({ contextId, contextName, onClose }: ExportModalProps) {
  const [exporting, setExporting] = useState<'context' | 'all' | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleExport(scope: 'context' | 'all') {
    setExporting(scope);
    setError(null);
    try {
      const url = scope === 'context' ? `/export?context_id=${contextId}` : '/export';
      const { data } = await apiClient.get(url);

      const date = new Date().toISOString().slice(0, 10);
      const filename =
        scope === 'context'
          ? `gtd-export-${sanitizeFilename(contextName)}-${date}.json`
          : `gtd-export-all-${date}.json`;

      triggerDownload(data, filename);
      onClose();
    } catch {
      setError('Failed to export data. Please try again.');
    } finally {
      setExporting(null);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={onClose}>
      <div
        className="w-full max-w-sm rounded-xl border border-stone-200 bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-base font-semibold text-stone-900">Export Data</h2>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <p className="mb-4 text-sm text-stone-500">
          Download your data as a JSON file. Deleted items are excluded.
        </p>

        <div className="flex flex-col gap-3">
          <button
            type="button"
            onClick={() => handleExport('context')}
            disabled={exporting !== null}
            className="flex items-center gap-3 rounded-lg border border-stone-200 px-4 py-3 text-left transition hover:border-stone-300 hover:bg-stone-50 disabled:opacity-50"
          >
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-stone-100 text-stone-600">
              <FileText className="h-4.5 w-4.5" />
            </div>
            <div>
              <div className="text-sm font-medium text-stone-900">
                {exporting === 'context' ? 'Exporting...' : 'This context'}
              </div>
              <div className="text-xs text-stone-500">
                Export &ldquo;{contextName}&rdquo; with all tasks and categories
              </div>
            </div>
          </button>

          <button
            type="button"
            onClick={() => handleExport('all')}
            disabled={exporting !== null}
            className="flex items-center gap-3 rounded-lg border border-stone-200 px-4 py-3 text-left transition hover:border-stone-300 hover:bg-stone-50 disabled:opacity-50"
          >
            <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-stone-100 text-stone-600">
              <Archive className="h-4.5 w-4.5" />
            </div>
            <div>
              <div className="text-sm font-medium text-stone-900">
                {exporting === 'all' ? 'Exporting...' : 'All contexts'}
              </div>
              <div className="text-xs text-stone-500">
                Export everything — all contexts, tasks, and categories
              </div>
            </div>
          </button>
        </div>

        {error && (
          <div className="mt-3 rounded-md bg-red-50 px-3 py-2 text-xs text-red-700">{error}</div>
        )}
      </div>
    </div>
  );
}
