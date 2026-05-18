import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import apiClient from '../api/client';
import type { ContextResponse } from '../types';

export default function ContextWorkspacePage() {
  const { contextId } = useParams<{ contextId: string }>();
  const [context, setContext] = useState<ContextResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!contextId) return;
    apiClient
      .get<ContextResponse>(`/contexts/${contextId}`)
      .then(({ data }) => setContext(data))
      .catch(() => navigate('/contexts', { replace: true }))
      .finally(() => setLoading(false));
  }, [contextId, navigate]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-stone-50">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-stone-300 border-t-stone-800" />
      </div>
    );
  }

  if (!context) return null;

  return (
    <div className="min-h-screen bg-stone-50">
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center gap-4 px-6 py-4">
          <button
            type="button"
            onClick={() => navigate('/contexts')}
            className="rounded-md p-1.5 text-stone-400 transition hover:bg-stone-100 hover:text-stone-700"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <div className="flex items-center gap-2">
            <span className="text-xl">{context.icon}</span>
            <h1 className="text-lg font-semibold text-stone-900">{context.name}</h1>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-10">
        <div className="rounded-xl border border-dashed border-stone-300 bg-white p-12 text-center">
          <p className="text-stone-500">
            Workspace for <strong>{context.name}</strong> — coming soon.
          </p>
          <p className="mt-2 text-sm text-stone-400">
            Task management UI will be implemented in the next iteration.
          </p>
        </div>
      </main>
    </div>
  );
}
