import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../api/client';
import { useAuthStore } from '../store/authStore';
import type { ContextResponse } from '../types';

export default function ContextsPage() {
  const [contexts, setContexts] = useState<ContextResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
      return;
    }
    apiClient
      .get<ContextResponse[]>('/contexts')
      .then(({ data }) => setContexts(data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [isAuthenticated, navigate]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-50">
        <p className="text-gray-500">Loading…</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="border-b border-gray-200 bg-white px-6 py-4">
        <div className="mx-auto flex max-w-4xl items-center justify-between">
          <h1 className="text-lg font-semibold text-gray-900">GTD</h1>
          <button
            type="button"
            onClick={() => {
              apiClient.post('/auth/logout').catch(() => {});
              useAuthStore.getState().logout();
              navigate('/login', { replace: true });
            }}
            className="text-sm text-gray-500 hover:text-gray-900"
          >
            Logout
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-6 py-10">
        <h2 className="mb-6 text-xl font-semibold text-gray-900">
          Your Contexts
        </h2>

        {contexts.length === 0 ? (
          <p className="text-gray-500">
            No contexts yet. Create your first one to get started.
          </p>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {contexts.map((ctx) => (
              <div
                key={ctx.id}
                className="cursor-pointer rounded-lg border border-gray-200 bg-white p-5 shadow-sm transition hover:shadow-md"
              >
                <div className="mb-2 text-2xl">{ctx.icon || '📋'}</div>
                <h3 className="font-medium text-gray-900">{ctx.name}</h3>
                <p className="mt-1 text-xs text-gray-400">
                  {ctx.theme.toLowerCase()}
                </p>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
