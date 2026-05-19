import { useEffect, useReducer, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus } from 'lucide-react';
import apiClient from '../api/client';
import { useAuthStore } from '../store/authStore';
import CreateContextModal from '../components/CreateContextModal';
import { createElement } from 'react';
import { getContextIcon } from '../utils/icons';
import type { ContextResponse, ContextTheme, TaskCountsResponse } from '../types';

const MAX_CONTEXTS = 5;

const THEME_STYLES: Record<ContextTheme, { card: string; badge: string; accent: string }> = {
  MINIMALIST: {
    card: 'bg-white border-stone-200 hover:border-stone-300',
    badge: 'bg-stone-100 text-stone-600',
    accent: 'text-stone-500',
  },
  DESIGN: {
    card: 'bg-violet-50/50 border-violet-200 hover:border-violet-300',
    badge: 'bg-violet-100 text-violet-600',
    accent: 'text-violet-500',
  },
  FORMAL: {
    card: 'bg-slate-50/50 border-slate-200 hover:border-slate-300',
    badge: 'bg-slate-100 text-slate-600',
    accent: 'text-slate-500',
  },
  NATURE: {
    card: 'bg-emerald-50/50 border-emerald-200 hover:border-emerald-300',
    badge: 'bg-emerald-100 text-emerald-600',
    accent: 'text-emerald-500',
  },
  DARK: {
    card: 'bg-zinc-900 border-zinc-700 hover:border-zinc-500',
    badge: 'bg-zinc-700 text-zinc-200',
    accent: 'text-zinc-400',
  },
  DRAGONS: {
    card: 'bg-stone-900 border-amber-800/40 hover:border-amber-600/60',
    badge: 'bg-amber-950/50 text-amber-300',
    accent: 'text-amber-500',
  },
};

async function loadContexts(): Promise<{ contexts: ContextResponse[]; inboxCounts: Record<string, number> }> {
  const { data } = await apiClient.get<ContextResponse[]>('/contexts');

  const counts: Record<string, number> = {};
  await Promise.all(
    data.map(async (ctx) => {
      try {
        const { data: countsData } = await apiClient.get<TaskCountsResponse>(
          `/contexts/${ctx.id}/tasks/counts`,
        );
        counts[ctx.id] = countsData.byGtdList['INBOX'] ?? 0;
      } catch {
        counts[ctx.id] = 0;
      }
    }),
  );

  return { contexts: data, inboxCounts: counts };
}

export default function ContextsPage() {
  const [contexts, setContexts] = useState<ContextResponse[]>([]);
  const [inboxCounts, setInboxCounts] = useState<Record<string, number>>({});
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [refreshKey, refresh] = useReducer((x: number) => x + 1, 0);
  const navigate = useNavigate();

  useEffect(() => {
    let cancelled = false;
    loadContexts()
      .then(({ contexts: ctxs, inboxCounts: counts }) => {
        if (cancelled) return;
        setContexts(ctxs);
        setInboxCounts(counts);
      })
      .catch(() => {})
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [refreshKey]);

  function handleLogout() {
    apiClient.post('/auth/logout').catch(() => {});
    useAuthStore.getState().logout();
    navigate('/login', { replace: true });
  }

  async function handleCreateContext(data: { name: string; theme: ContextTheme; icon: string }) {
    await apiClient.post('/contexts', data);
  }

  function handleContextClick(contextId: string) {
    navigate(`/contexts/${contextId}`);
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

  const canCreateMore = contexts.length < MAX_CONTEXTS;

  return (
    <div className="min-h-screen bg-stone-50">
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2.5">
            <div className="flex h-7 w-7 items-center justify-center rounded-md bg-stone-900 text-xs font-bold text-white">
              G
            </div>
            <h1 className="text-lg font-semibold tracking-tight text-stone-900">GTD</h1>
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
        <div className="mb-8 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-stone-900">Your Contexts</h2>
          {canCreateMore && (
            <button
              type="button"
              onClick={() => setModalOpen(true)}
              className="flex items-center gap-1.5 rounded-lg bg-stone-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-stone-800"
            >
              <Plus className="h-4 w-4" />
              New Context
            </button>
          )}
        </div>

        {contexts.length === 0 ? (
          <div className="rounded-xl border border-dashed border-stone-300 bg-white p-12 text-center">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-stone-100">
              <Plus className="h-7 w-7 text-stone-400" />
            </div>
            <h3 className="mb-1 text-base font-medium text-stone-900">No contexts yet</h3>
            <p className="mb-5 text-sm text-stone-500">
              Contexts are separate workspaces for different areas of your life.
            </p>
            <button
              type="button"
              onClick={() => setModalOpen(true)}
              className="rounded-lg bg-stone-900 px-5 py-2.5 text-sm font-medium text-white transition hover:bg-stone-800"
            >
              Create your first context
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {contexts.map((ctx) => {
              const style = THEME_STYLES[ctx.theme];
              const inboxCount = inboxCounts[ctx.id] ?? 0;
              const isDark = ctx.theme === 'DARK' || ctx.theme === 'DRAGONS';
              return (
                <button
                  key={ctx.id}
                  type="button"
                  onClick={() => handleContextClick(ctx.id)}
                  className={`group cursor-pointer rounded-xl border p-5 text-left shadow-sm transition hover:shadow-md ${style.card}`}
                >
                  <div className="mb-3">
                    {createElement(getContextIcon(ctx.icon), { className: `h-8 w-8 ${isDark ? 'text-zinc-300' : 'text-stone-600'}` })}
                  </div>
                  <h3 className={`text-base font-medium ${isDark ? 'text-white' : 'text-stone-900'}`}>
                    {ctx.name}
                  </h3>
                  <div className="mt-3 flex items-center justify-between">
                    <span className={`text-xs ${style.accent}`}>
                      {ctx.theme.charAt(0) + ctx.theme.slice(1).toLowerCase()}
                    </span>
                    {inboxCount > 0 && (
                      <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${style.badge}`}>
                        {inboxCount} in Inbox
                      </span>
                    )}
                  </div>
                </button>
              );
            })}
          </div>
        )}

        {!canCreateMore && contexts.length > 0 && (
          <p className="mt-4 text-center text-xs text-stone-400">
            Maximum of {MAX_CONTEXTS} contexts reached
          </p>
        )}
      </main>

      <CreateContextModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        onCreated={refresh}
        onSubmit={handleCreateContext}
      />
    </div>
  );
}
