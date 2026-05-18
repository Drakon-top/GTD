import { useCallback, useEffect, useReducer, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import apiClient from '../api/client';
import type {
  CategoryResponse,
  ContextResponse,
  GtdList,
  TaskCountsResponse,
  TaskResponse,
} from '../types';
import Sidebar from '../components/Sidebar';
import TaskList from '../components/TaskList';
import { gtdListLabel } from '../utils/gtdLabels';
import TaskDetailPanel from '../components/TaskDetailPanel';

export default function ContextWorkspacePage() {
  const { contextId } = useParams<{ contextId: string }>();
  const navigate = useNavigate();

  const [context, setContext] = useState<ContextResponse | null>(null);
  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [counts, setCounts] = useState<TaskCountsResponse | null>(null);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeSection, setActiveSection] = useState<string>('INBOX');
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null);
  const [refreshKey, refresh] = useReducer((x: number) => x + 1, 0);

  useEffect(() => {
    if (!contextId) return;
    let cancelled = false;

    async function load() {
      try {
        const [ctxRes, catsRes, countsRes] = await Promise.all([
          apiClient.get<ContextResponse>(`/contexts/${contextId}`),
          apiClient.get<CategoryResponse[]>(`/contexts/${contextId}/categories`),
          apiClient.get<TaskCountsResponse>(`/contexts/${contextId}/tasks/counts`),
        ]);
        if (cancelled) return;
        setContext(ctxRes.data);
        setCategories(catsRes.data);
        setCounts(countsRes.data);
      } catch {
        if (!cancelled) navigate('/contexts', { replace: true });
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [contextId, navigate, refreshKey]);

  useEffect(() => {
    if (!contextId) return;
    let cancelled = false;

    async function loadTasks() {
      try {
        const isCat = activeSection.startsWith('cat:');
        let url: string;
        if (isCat) {
          url = `/contexts/${contextId}/tasks`;
        } else {
          url = `/contexts/${contextId}/tasks?gtd_list=${activeSection}`;
        }
        const { data } = await apiClient.get<TaskResponse[]>(url);
        if (cancelled) return;

        if (isCat) {
          const catId = activeSection.slice(4);
          setTasks(data.filter((t) => t.categoryId === catId));
        } else {
          setTasks(data);
        }
      } catch {
        if (!cancelled) setTasks([]);
      }
    }
    loadTasks();
    return () => { cancelled = true; };
  }, [contextId, activeSection, refreshKey]);

  const handleTasksChanged = useCallback(() => {
    refresh();
  }, []);

  const handleTaskClick = useCallback((task: TaskResponse) => {
    setSelectedTaskId((prev) => (prev === task.id ? null : task.id));
  }, []);

  const handleCloseDetail = useCallback(() => {
    setSelectedTaskId(null);
  }, []);

  function sectionLabel(): string {
    if (activeSection.startsWith('cat:')) {
      const catId = activeSection.slice(4);
      const cat = categories.find((c) => c.id === catId);
      return cat ? cat.name : 'Category';
    }
    return gtdListLabel(activeSection as GtdList);
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-stone-50">
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-stone-300 border-t-stone-800" />
      </div>
    );
  }

  if (!context || !contextId) return null;

  return (
    <div className="flex h-screen flex-col bg-stone-50">
      <header className="shrink-0 border-b border-stone-200 bg-white">
        <div className="flex items-center gap-4 px-4 py-2.5">
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
            <span className="text-lg">{context.icon}</span>
            <h1 className="text-base font-semibold text-stone-900">{context.name}</h1>
          </div>
          {counts && (
            <span className="ml-auto text-xs text-stone-400">
              {counts.total} task{counts.total !== 1 ? 's' : ''}
            </span>
          )}
        </div>
      </header>

      <div className="flex min-h-0 flex-1">
        <Sidebar
          counts={counts}
          categories={categories}
          activeSection={activeSection}
          onSectionChange={setActiveSection}
        />

        <main className="min-w-0 flex-1 bg-white">
          <TaskList
            tasks={tasks}
            sectionLabel={sectionLabel()}
            contextId={contextId}
            onTaskClick={handleTaskClick}
            onTasksChanged={handleTasksChanged}
            selectedTaskId={selectedTaskId}
          />
        </main>

        {selectedTaskId && (
          <TaskDetailPanel
            taskId={selectedTaskId}
            contextId={contextId}
            categories={categories}
            onClose={handleCloseDetail}
            onTaskChanged={handleTasksChanged}
          />
        )}
      </div>
    </div>
  );
}
