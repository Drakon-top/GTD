import { useCallback, useEffect, useReducer, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { DndContext, DragOverlay, PointerSensor, useSensor, useSensors } from '@dnd-kit/core';
import type { DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import { ArrowLeft, Download, Paintbrush, Check } from 'lucide-react';
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
import DragOverlayCard from '../components/DragOverlayCard';
import CategoryManager from '../components/CategoryManager';
import ExportModal from '../components/ExportModal';
import { getTheme, THEME_NAMES } from '../utils/themes';
import type { ThemeColors } from '../utils/themes';
import type { ContextTheme } from '../types';
import { createElement } from 'react';
import { getContextIcon } from '../utils/icons';

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
  const [draggedTask, setDraggedTask] = useState<TaskResponse | null>(null);
  const [showCategoryManager, setShowCategoryManager] = useState(false);
  const [showThemePicker, setShowThemePicker] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } })
  );

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

  function handleDragStart(event: DragStartEvent) {
    const { task } = event.active.data.current as { task: TaskResponse };
    setDraggedTask(task);
  }

  async function handleDragEnd(event: DragEndEvent) {
    setDraggedTask(null);
    const { active, over } = event;
    if (!over) return;

    const overId = over.id as string;
    if (!overId.startsWith('gtd:')) return;

    const targetList = overId.slice(4) as GtdList;
    const { task } = active.data.current as { task: TaskResponse };

    if (task.gtdList === targetList) return;
    if (task.isCompleted) return;

    try {
      await apiClient.patch(`/tasks/${task.id}/move`, { gtdList: targetList });
      refresh();
    } catch { /* silently fail */ }
  }

  async function handleThemeChange(newTheme: ContextTheme) {
    if (!context || context.theme === newTheme) return;
    try {
      await apiClient.put(`/contexts/${context.id}`, { theme: newTheme });
      setContext((prev) => prev ? { ...prev, theme: newTheme } : prev);
      setShowThemePicker(false);
    } catch { /* silently fail */ }
  }

  function sectionLabel(): string {
    if (activeSection.startsWith('cat:')) {
      const catId = activeSection.slice(4);
      const cat = categories.find((c) => c.id === catId);
      return cat ? cat.name : 'Category';
    }
    return gtdListLabel(activeSection as GtdList);
  }

  const theme: ThemeColors = context ? getTheme(context.theme) : getTheme('MINIMALIST');

  if (loading) {
    return (
      <div className={`flex min-h-screen items-center justify-center ${theme.bg}`}>
        <div className="h-6 w-6 animate-spin rounded-full border-2 border-stone-300 border-t-stone-800" />
      </div>
    );
  }

  if (!context || !contextId) return null;

  return (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <div className={`flex h-screen flex-col ${theme.bg} ${theme.fontFamily}`}>
        <header className={`shrink-0 border-b ${theme.headerBorder} ${theme.headerBg} ${theme.shadow} ${theme.headerStyle}`}>
          <div className={`flex items-center gap-4 ${theme.spacing}`}>
            <button
              type="button"
              onClick={() => navigate('/contexts')}
              className={`${theme.borderRadius} p-1.5 ${theme.headerSubtext} transition hover:opacity-80`}
            >
              <ArrowLeft className="h-5 w-5" />
            </button>
            <div className="flex items-center gap-2">
              {createElement(getContextIcon(context.icon), { className: `h-5 w-5 ${theme.headerText}` })}
              <h1 className={`${theme.headerTitleSize} font-semibold ${theme.headerFont} ${theme.headerText} ${theme.glowText}`}>{context.name}</h1>
            </div>
            <div className="ml-auto flex items-center gap-3">
              {counts && (
                <span className={`text-xs ${theme.headerSubtext}`}>
                  {counts.total} task{counts.total !== 1 ? 's' : ''}
                </span>
              )}
              <button
                type="button"
                onClick={() => setShowExportModal(true)}
                className={`${theme.borderRadius} p-1.5 ${theme.headerSubtext} transition hover:opacity-80`}
                title="Export data"
              >
                <Download className="h-4 w-4" />
              </button>
              <div className="relative">
                <button
                  type="button"
                  onClick={() => setShowThemePicker(!showThemePicker)}
                  className={`${theme.borderRadius} p-1.5 ${theme.headerSubtext} transition hover:opacity-80`}
                  title="Change theme"
                >
                  <Paintbrush className="h-4 w-4" />
                </button>
                {showThemePicker && (
                  <div className={`absolute right-0 top-full z-50 mt-1 w-40 ${theme.borderRadiusLg} ${theme.decorativeBorder} ${theme.inputBorder} ${theme.panelBg} py-1 ${theme.shadowLg}`}>
                    {(Object.keys(THEME_NAMES) as ContextTheme[]).map((t) => (
                      <button
                        key={t}
                        type="button"
                        onClick={() => handleThemeChange(t)}
                        className={`flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm transition ${theme.transitionSpeed} ${theme.taskHover} ${
                          context.theme === t ? `font-medium ${theme.taskText}` : theme.taskSubtext
                        }`}
                      >
                        <span className={`h-3 w-3 rounded-full ${
                          t === 'MINIMALIST' ? 'bg-stone-400'
                          : t === 'DESIGN' ? 'bg-violet-400'
                          : t === 'FORMAL' ? 'bg-slate-400'
                          : t === 'NATURE' ? 'bg-emerald-400'
                          : 'bg-zinc-700'
                        }`} />
                        {THEME_NAMES[t]}
                        {context.theme === t && <Check className="ml-auto h-3 w-3" />}
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </header>

        <div className="flex min-h-0 flex-1">
          <Sidebar
            counts={counts}
            categories={categories}
            activeSection={activeSection}
            onSectionChange={setActiveSection}
            onManageCategories={() => setShowCategoryManager(true)}
            theme={theme}
          />

          <main className={`min-w-0 flex-1 ${theme.mainBg} ${theme.bgPattern} ${theme.scanlineOverlay ? 'theme-dark-scanlines' : ''}`}>
            <TaskList
              tasks={tasks}
              sectionLabel={sectionLabel()}
              contextId={contextId}
              onTaskClick={handleTaskClick}
              onTasksChanged={handleTasksChanged}
              selectedTaskId={selectedTaskId}
              theme={theme}
            />
          </main>

          {selectedTaskId && (
            <TaskDetailPanel
              taskId={selectedTaskId}
              contextId={contextId}
              categories={categories}
              onClose={handleCloseDetail}
              onTaskChanged={handleTasksChanged}
              theme={theme}
            />
          )}
        </div>
      </div>

      <DragOverlay>
        {draggedTask && <DragOverlayCard task={draggedTask} theme={theme} />}
      </DragOverlay>

      {showCategoryManager && (
        <CategoryManager
          contextId={contextId}
          categories={categories}
          onCategoriesChanged={() => { refresh(); }}
          onClose={() => setShowCategoryManager(false)}
          theme={theme}
        />
      )}

      {showExportModal && (
        <ExportModal
          contextId={contextId}
          contextName={context.name}
          onClose={() => setShowExportModal(false)}
          theme={theme}
        />
      )}
    </DndContext>
  );
}
