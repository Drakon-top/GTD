import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../api/client';
import { useAuthStore } from '../store/authStore';
import type { ContextResponse } from '../types';

export default function ContextsPage() {
  const [contexts, setContexts] = useState<ContextResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    apiClient
      .get<ContextResponse[]>('/contexts')
      .then(({ data }) => setContexts(data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  function handleLogout() {
    apiClient.post('/auth/logout').catch(() => {});
    useAuthStore.getState().logout();
    navigate('/login', { replace: true });
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-stone-50">
        <div className="flex flex-col items-center gap-3">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-stone-300 border-t-stone-800" />
          <p className="text-sm text-stone-400">Loading contexts...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-stone-50">
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-7 w-7 items-center justify-center rounded-md bg-stone-900 text-xs font-bold text-white">
              G
            </div>
            <h1 className="text-lg font-semibold tracking-tight text-stone-900">
              GTD
            </h1>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-md px-3 py-1.5 text-sm text-stone-500 transition hover:bg-stone-100 hover:text-stone-900"
          >
            Sign out
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-6 py-10">
        <h2 className="mb-6 text-xl font-semibold text-stone-900">
          Your Contexts
        </h2>

        {contexts.length === 0 ? (
          <div className="rounded-xl border border-dashed border-stone-300 bg-white p-10 text-center">
            <p className="text-stone-500">
              No contexts yet. Create your first one to get started.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {contexts.map((ctx) => (
              <div
                key={ctx.id}
                className="cursor-pointer rounded-xl border border-stone-200 bg-white p-5 shadow-sm transition hover:border-stone-300 hover:shadow-md"
              >
                <div className="mb-2 text-2xl">{ctx.icon || '📋'}</div>
                <h3 className="font-medium text-stone-900">{ctx.name}</h3>
                <p className="mt-1 text-xs text-stone-400">
                  {ctx.theme.charAt(0) + ctx.theme.slice(1).toLowerCase()}
                </p>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
