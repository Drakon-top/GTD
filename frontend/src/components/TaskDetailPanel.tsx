import { useCallback, useEffect, useState } from 'react';
import apiClient from '../api/client';
import type { CategoryResponse, GtdList, TaskResponse } from '../types';
import { gtdListLabel } from '../utils/gtdLabels';

interface TaskDetailPanelProps {
  taskId: string;
  contextId: string;
  categories: CategoryResponse[];
  onClose: () => void;
  onTaskChanged: () => void;
}

const GTD_OPTIONS: GtdList[] = [
  'INBOX', 'NEXT_ACTIONS', 'PROJECTS', 'WAITING_FOR',
  'SOMEDAY_MAYBE', 'REFERENCE', 'CALENDAR',
];

async function fetchTask(taskId: string): Promise<TaskResponse> {
  const { data } = await apiClient.get<TaskResponse>(`/tasks/${taskId}`);
  return data;
}

export default function TaskDetailPanel({
  taskId,
  contextId,
  categories,
  onClose,
  onTaskChanged,
}: TaskDetailPanelProps) {
  const [task, setTask] = useState<TaskResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [editingTitle, setEditingTitle] = useState(false);
  const [titleDraft, setTitleDraft] = useState('');
  const [notesDraft, setNotesDraft] = useState('');
  const [savingNotes, setSavingNotes] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [loadedTaskId, setLoadedTaskId] = useState<string | null>(null);

  if (taskId !== loadedTaskId) {
    setLoadedTaskId(taskId);
    setLoading(true);
    setConfirmDelete(false);
    setEditingTitle(false);
  }

  useEffect(() => {
    let cancelled = false;
    fetchTask(taskId)
      .then((data) => {
        if (cancelled) return;
        setTask(data);
        setTitleDraft(data.title);
        setNotesDraft(data.notes ?? '');
      })
      .catch(() => {
        if (!cancelled) onClose();
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => { cancelled = true; };
  }, [taskId, onClose]);

  async function saveTitle() {
    if (!task || titleDraft.trim() === task.title) {
      setEditingTitle(false);
      return;
    }
    try {
      await apiClient.put(`/tasks/${task.id}`, { title: titleDraft.trim() });
      setEditingTitle(false);
      setTask((prev) => prev ? { ...prev, title: titleDraft.trim() } : prev);
      onTaskChanged();
    } catch {
      setTitleDraft(task.title);
      setEditingTitle(false);
    }
  }

  async function saveNotes() {
    if (!task || notesDraft === (task.notes ?? '')) return;
    setSavingNotes(true);
    try {
      await apiClient.put(`/tasks/${task.id}`, { notes: notesDraft || null });
      setTask((prev) => prev ? { ...prev, notes: notesDraft || null } : prev);
      onTaskChanged();
    } catch {
      setNotesDraft(task.notes ?? '');
    } finally {
      setSavingNotes(false);
    }
  }

  async function handleMove(gtdList: GtdList) {
    if (!task || task.gtdList === gtdList) return;
    try {
      await apiClient.patch(`/tasks/${task.id}/move`, { gtdList });
      setTask((prev) => prev ? { ...prev, gtdList } : prev);
      onTaskChanged();
    } catch { /* silently fail */ }
  }

  async function handleDueDateChange(dateStr: string) {
    if (!task) return;
    const dueDate = dateStr ? new Date(dateStr + 'T23:59:59Z').toISOString() : null;
    try {
      await apiClient.put(`/tasks/${task.id}`, { dueDate });
      setTask((prev) => prev ? { ...prev, dueDate } : prev);
      onTaskChanged();
    } catch { /* silently fail */ }
  }

  async function handleCategoryChange(categoryId: string) {
    if (!task) return;
    const value = categoryId || null;
    try {
      await apiClient.put(`/tasks/${task.id}`, { categoryId: value });
      setTask((prev) => prev ? { ...prev, categoryId: value } : prev);
      onTaskChanged();
    } catch { /* silently fail */ }
  }

  async function handleDelete() {
    if (!task) return;
    try {
      await apiClient.delete(`/tasks/${task.id}`);
      onTaskChanged();
      onClose();
    } catch { /* silently fail */ }
  }

  async function handleComplete() {
    if (!task) return;
    try {
      await apiClient.patch(`/tasks/${task.id}/complete`);
      onTaskChanged();
      onClose();
    } catch { /* silently fail */ }
  }

  const reloadTask = useCallback(async () => {
    const updated = await fetchTask(taskId);
    setTask(updated);
    onTaskChanged();
  }, [taskId, onTaskChanged]);

  async function handleSubtaskToggle(subtaskId: string) {
    try {
      await apiClient.patch(`/tasks/${subtaskId}/complete`);
      await reloadTask();
    } catch { /* silently fail */ }
  }

  async function handleAddSubtask(parentId: string, title: string) {
    try {
      await apiClient.post(`/tasks/${parentId}/subtasks`, { title });
      await reloadTask();
    } catch { /* silently fail */ }
  }

  function dueDateInputValue(): string {
    if (!task?.dueDate) return '';
    return task.dueDate.slice(0, 10);
  }

  if (loading) {
    return (
      <div className="flex h-full w-80 shrink-0 items-center justify-center border-l border-stone-200 bg-white">
        <div className="h-5 w-5 animate-spin rounded-full border-2 border-stone-300 border-t-stone-800" />
      </div>
    );
  }

  if (!task) return null;

  const categoryName = task.categoryId
    ? categories.find((c) => c.id === task.categoryId)?.name ?? null
    : null;

  return (
    <div className="flex h-full w-80 shrink-0 flex-col border-l border-stone-200 bg-white">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-stone-200 px-4 py-3">
        <span className="text-xs font-medium uppercase tracking-wide text-stone-400">Details</span>
        <button
          type="button"
          onClick={onClose}
          className="rounded-md p-1 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
          aria-label="Close details"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      {/* Body */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        {/* Title */}
        {editingTitle ? (
          <input
            autoFocus
            value={titleDraft}
            onChange={(e) => setTitleDraft(e.target.value)}
            onBlur={saveTitle}
            onKeyDown={(e) => {
              if (e.key === 'Enter') saveTitle();
              if (e.key === 'Escape') { setTitleDraft(task.title); setEditingTitle(false); }
            }}
            className="mb-3 w-full rounded border border-stone-300 px-2 py-1 text-sm font-semibold text-stone-900 outline-none focus:border-stone-500"
          />
        ) : (
          <h3
            onClick={() => { if (!task.isCompleted) setEditingTitle(true); }}
            className={`mb-3 rounded px-1 -mx-1 text-sm font-semibold ${
              task.isCompleted
                ? 'text-stone-400 line-through'
                : 'cursor-pointer text-stone-900 hover:bg-stone-50'
            }`}
          >
            {task.title}
          </h3>
        )}

        {/* GTD List */}
        <div className="mb-4">
          <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
            GTD List
          </label>
          <select
            value={task.gtdList}
            onChange={(e) => handleMove(e.target.value as GtdList)}
            disabled={task.isCompleted}
            className="w-full rounded-md border border-stone-200 bg-white px-2 py-1.5 text-sm text-stone-700 outline-none focus:border-stone-400 disabled:opacity-50"
          >
            {GTD_OPTIONS.map((g) => (
              <option key={g} value={g}>{gtdListLabel(g)}</option>
            ))}
            {task.isCompleted && <option value="DONE">Done</option>}
          </select>
        </div>

        {/* Due Date */}
        <div className="mb-4">
          <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
            Due Date
          </label>
          <div className="flex items-center gap-2">
            <input
              type="date"
              value={dueDateInputValue()}
              onChange={(e) => handleDueDateChange(e.target.value)}
              disabled={task.isCompleted}
              className="flex-1 rounded-md border border-stone-200 bg-white px-2 py-1.5 text-sm text-stone-700 outline-none focus:border-stone-400 disabled:opacity-50"
            />
            {task.dueDate && !task.isCompleted && (
              <button
                type="button"
                onClick={() => handleDueDateChange('')}
                className="rounded-md p-1.5 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
                title="Clear due date"
              >
                <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </div>
        </div>

        {/* Category */}
        <div className="mb-4">
          <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
            Category
          </label>
          <select
            value={task.categoryId ?? ''}
            onChange={(e) => handleCategoryChange(e.target.value)}
            disabled={task.isCompleted}
            className="w-full rounded-md border border-stone-200 bg-white px-2 py-1.5 text-sm text-stone-700 outline-none focus:border-stone-400 disabled:opacity-50"
          >
            <option value="">No category</option>
            {categories.map((cat) => (
              <option key={cat.id} value={cat.id}>
                {cat.icon ? `${cat.icon} ` : ''}{cat.name}
              </option>
            ))}
          </select>
          {categoryName && (
            <p className="mt-1 text-xs text-stone-400">
              Currently: {categoryName}
            </p>
          )}
        </div>

        {/* Notes */}
        <div className="mb-4">
          <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
            Notes
          </label>
          <textarea
            value={notesDraft}
            onChange={(e) => setNotesDraft(e.target.value)}
            onBlur={saveNotes}
            rows={4}
            placeholder="Add notes..."
            disabled={task.isCompleted}
            className="w-full resize-none rounded-md border border-stone-200 px-2 py-1.5 text-sm text-stone-700 outline-none placeholder:text-stone-400 focus:border-stone-400 disabled:opacity-50"
          />
          {savingNotes && <p className="mt-0.5 text-[10px] text-stone-400">Saving...</p>}
        </div>

        {/* Progress */}
        {task.progress != null && (
          <div className="mb-4">
            <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
              Progress
            </label>
            <div className="flex items-center gap-2">
              <div className="h-2 flex-1 overflow-hidden rounded-full bg-stone-200">
                <div
                  className="h-full rounded-full bg-stone-600 transition-all"
                  style={{ width: `${task.progress}%` }}
                />
              </div>
              <span className="text-xs font-medium text-stone-600">{task.progress}%</span>
            </div>
          </div>
        )}

        {/* Subtasks */}
        <SubtaskSection
          parentId={task.id}
          subtasks={task.subtasks ?? []}
          nestingLevel={task.nestingLevel}
          isCompleted={task.isCompleted}
          onToggle={handleSubtaskToggle}
          onAdd={handleAddSubtask}
        />

        {/* Recurring */}
        {task.isRecurring && (
          <div className="mb-4">
            <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
              Recurring
            </label>
            <p className="text-sm text-stone-600">🔁 This task repeats</p>
          </div>
        )}

        {/* Metadata */}
        <div className="mb-2 text-[10px] text-stone-400">
          Created {new Date(task.createdAt).toLocaleDateString('en-US', {
            month: 'short', day: 'numeric', year: 'numeric',
          })}
          {' · '}v{task.version}
          {contextId && ` · ${contextId.slice(0, 8)}…`}
        </div>
      </div>

      {/* Actions */}
      <div className="border-t border-stone-200 px-4 py-3">
        {confirmDelete ? (
          <div className="space-y-2">
            <p className="text-xs text-stone-500">Are you sure? This cannot be undone.</p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleDelete}
                className="flex-1 rounded-md bg-red-600 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-red-700"
              >
                Delete
              </button>
              <button
                type="button"
                onClick={() => setConfirmDelete(false)}
                className="flex-1 rounded-md border border-stone-200 px-3 py-1.5 text-sm font-medium text-stone-600 transition hover:bg-stone-50"
              >
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <div className="flex gap-2">
            {!task.isCompleted && (
              <button
                type="button"
                onClick={handleComplete}
                className="flex-1 rounded-md bg-stone-900 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-stone-800"
              >
                Complete
              </button>
            )}
            <button
              type="button"
              onClick={() => setConfirmDelete(true)}
              className="rounded-md border border-red-200 px-3 py-1.5 text-sm font-medium text-red-600 transition hover:bg-red-50"
            >
              Delete
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

function SubtaskSection({
  parentId,
  subtasks,
  nestingLevel,
  isCompleted,
  onToggle,
  onAdd,
}: {
  parentId: string;
  subtasks: TaskResponse[];
  nestingLevel: number;
  isCompleted: boolean;
  onToggle: (subtaskId: string) => void;
  onAdd: (parentId: string, title: string) => void;
}) {
  const canAddMore = nestingLevel < 4;
  const completedCount = countCompleted(subtasks);
  const totalCount = countTotal(subtasks);

  return (
    <div className="mb-4">
      <div className="mb-1 flex items-center justify-between">
        <label className="text-[11px] font-medium uppercase tracking-wide text-stone-400">
          Subtasks
          {totalCount > 0 && (
            <span className="ml-1 normal-case">
              ({completedCount}/{totalCount})
            </span>
          )}
        </label>
      </div>

      {totalCount > 0 && (
        <div className="mb-2 flex items-center gap-2">
          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-stone-200">
            <div
              className="h-full rounded-full bg-emerald-500 transition-all"
              style={{ width: `${totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0}%` }}
            />
          </div>
          <span className="text-[10px] font-medium text-stone-500">
            {totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0}%
          </span>
        </div>
      )}

      {subtasks.length > 0 && (
        <SubtaskTree
          subtasks={subtasks}
          depth={0}
          parentIsCompleted={isCompleted}
          onToggle={onToggle}
          onAdd={onAdd}
        />
      )}

      {subtasks.length === 0 && (
        <p className="text-xs text-stone-400">
          {canAddMore && !isCompleted ? 'No subtasks yet.' : 'No subtasks.'}
        </p>
      )}

      {canAddMore && !isCompleted && (
        <AddSubtaskInline parentId={parentId} onAdd={onAdd} depth={0} />
      )}

      {!canAddMore && !isCompleted && (
        <p className="mt-1 text-[10px] text-amber-600">
          Maximum nesting depth reached (4 levels). Cannot add deeper subtasks.
        </p>
      )}
    </div>
  );
}

function countCompleted(subtasks: TaskResponse[]): number {
  let count = 0;
  for (const s of subtasks) {
    if (s.isCompleted) count++;
    if (s.subtasks && s.subtasks.length > 0) count += countCompleted(s.subtasks);
  }
  return count;
}

function countTotal(subtasks: TaskResponse[]): number {
  let count = subtasks.length;
  for (const s of subtasks) {
    if (s.subtasks && s.subtasks.length > 0) count += countTotal(s.subtasks);
  }
  return count;
}

function SubtaskTree({
  subtasks,
  depth,
  parentIsCompleted,
  onToggle,
  onAdd,
}: {
  subtasks: TaskResponse[];
  depth: number;
  parentIsCompleted: boolean;
  onToggle: (subtaskId: string) => void;
  onAdd: (parentId: string, title: string) => void;
}) {
  return (
    <ul className={depth > 0 ? 'ml-4 border-l border-stone-100 pl-2' : ''}>
      {subtasks.map((sub) => (
        <SubtaskItem
          key={sub.id}
          subtask={sub}
          depth={depth}
          parentIsCompleted={parentIsCompleted}
          onToggle={onToggle}
          onAdd={onAdd}
        />
      ))}
    </ul>
  );
}

function SubtaskItem({
  subtask,
  depth,
  parentIsCompleted,
  onToggle,
  onAdd,
}: {
  subtask: TaskResponse;
  depth: number;
  parentIsCompleted: boolean;
  onToggle: (subtaskId: string) => void;
  onAdd: (parentId: string, title: string) => void;
}) {
  const [expanded, setExpanded] = useState(true);
  const hasChildren = subtask.subtasks && subtask.subtasks.length > 0;
  const canNest = subtask.nestingLevel < 4;

  return (
    <li className="py-0.5">
      <div className="group flex items-center gap-1.5 rounded px-1 py-0.5">
        {/* Expand/collapse toggle */}
        {hasChildren ? (
          <button
            type="button"
            onClick={() => setExpanded(!expanded)}
            className="flex h-4 w-4 shrink-0 items-center justify-center rounded text-stone-400 hover:bg-stone-100 hover:text-stone-600"
          >
            <svg
              className={`h-3 w-3 transition-transform ${expanded ? 'rotate-90' : ''}`}
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={2}
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        ) : (
          <span className="w-4 shrink-0" />
        )}

        {/* Checkbox */}
        <button
          type="button"
          onClick={() => { if (!subtask.isCompleted) onToggle(subtask.id); }}
          disabled={subtask.isCompleted}
          className={`h-3.5 w-3.5 shrink-0 rounded-sm border transition ${
            subtask.isCompleted
              ? 'border-stone-300 bg-stone-200 text-stone-500'
              : 'border-stone-300 hover:border-stone-500 cursor-pointer'
          } flex items-center justify-center`}
        >
          {subtask.isCompleted && (
            <svg className="h-2.5 w-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          )}
        </button>

        {/* Title */}
        <span className={`flex-1 text-sm ${subtask.isCompleted ? 'text-stone-400 line-through' : 'text-stone-700'}`}>
          {subtask.title}
        </span>

        {/* Subtask count badge */}
        {hasChildren && (
          <span className="text-[10px] text-stone-400">
            {subtask.subtasks!.filter((s) => s.isCompleted).length}/{subtask.subtasks!.length}
          </span>
        )}
      </div>

      {/* Nested subtasks */}
      {hasChildren && expanded && (
        <SubtaskTree
          subtasks={subtask.subtasks!}
          depth={depth + 1}
          parentIsCompleted={parentIsCompleted || subtask.isCompleted}
          onToggle={onToggle}
          onAdd={onAdd}
        />
      )}

      {/* Add subtask at this level */}
      {expanded && canNest && !parentIsCompleted && !subtask.isCompleted && (
        <div className="ml-4 border-l border-stone-100 pl-2">
          <AddSubtaskInline parentId={subtask.id} onAdd={onAdd} depth={depth + 1} />
        </div>
      )}
    </li>
  );
}

function AddSubtaskInline({
  parentId,
  onAdd,
  depth,
}: {
  parentId: string;
  onAdd: (parentId: string, title: string) => void;
  depth: number;
}) {
  const [adding, setAdding] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const title = newTitle.trim();
    if (!title) return;
    setSubmitting(true);
    try {
      onAdd(parentId, title);
      setNewTitle('');
      setAdding(false);
    } finally {
      setSubmitting(false);
    }
  }

  if (!adding) {
    return (
      <button
        type="button"
        onClick={() => setAdding(true)}
        className={`mt-0.5 rounded px-1.5 py-0.5 text-[11px] font-medium text-stone-400 transition hover:bg-stone-100 hover:text-stone-600 ${depth > 0 ? '' : 'mt-1'}`}
      >
        + Add subtask
      </button>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="mt-1 flex items-center gap-1.5">
      <input
        autoFocus
        type="text"
        value={newTitle}
        onChange={(e) => setNewTitle(e.target.value)}
        onKeyDown={(e) => { if (e.key === 'Escape') { setAdding(false); setNewTitle(''); } }}
        placeholder="Subtask title..."
        className="flex-1 rounded border border-stone-200 px-2 py-1 text-xs text-stone-800 outline-none placeholder:text-stone-400 focus:border-stone-400"
        disabled={submitting}
      />
      <button
        type="submit"
        disabled={submitting || !newTitle.trim()}
        className="rounded bg-stone-900 px-2 py-1 text-[10px] font-medium text-white transition hover:bg-stone-800 disabled:opacity-40"
      >
        Add
      </button>
      <button
        type="button"
        onClick={() => { setAdding(false); setNewTitle(''); }}
        className="rounded px-1.5 py-0.5 text-[10px] text-stone-500 transition hover:bg-stone-100"
      >
        ✕
      </button>
    </form>
  );
}
