import { useCallback, useEffect, useState } from 'react';
import apiClient from '../api/client';
import type { CategoryResponse, GtdList, ReminderResponse, TaskResponse } from '../types';
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

        {/* Reminders */}
        <ReminderSection
          taskId={task.id}
          isCompleted={task.isCompleted}
        />

        {/* Recurrence */}
        <RecurrenceSection
          task={task}
          onRecurrenceChanged={(rule) => {
            setTask((prev) => prev ? { ...prev, recurrenceRule: rule, isRecurring: !!rule } : prev);
            onTaskChanged();
          }}
        />

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

// --- Reminder Section ---

type OffsetPreset = { label: string; offsetType: string; offsetValue: number };
const OFFSET_PRESETS: OffsetPreset[] = [
  { label: '15 min before', offsetType: 'MINUTES_BEFORE', offsetValue: 15 },
  { label: '1 hour before', offsetType: 'HOURS_BEFORE', offsetValue: 1 },
  { label: '1 day before', offsetType: 'DAYS_BEFORE', offsetValue: 1 },
  { label: '3 days before', offsetType: 'DAYS_BEFORE', offsetValue: 3 },
];

function ReminderSection({ taskId, isCompleted }: { taskId: string; isCompleted: boolean }) {
  const [reminders, setReminders] = useState<ReminderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAdd, setShowAdd] = useState(false);
  const [addMode, setAddMode] = useState<'exact' | 'offset'>('exact');
  const [remindAt, setRemindAt] = useState('');
  const [selectedPreset, setSelectedPreset] = useState(0);
  const [submitting, setSubmitting] = useState(false);
  const [loadedTaskId, setLoadedTaskId] = useState<string | null>(null);

  if (taskId !== loadedTaskId) {
    setLoadedTaskId(taskId);
    setLoading(true);
    setShowAdd(false);
  }

  useEffect(() => {
    let cancelled = false;
    apiClient.get<ReminderResponse[]>(`/tasks/${taskId}/reminders`)
      .then(({ data }) => { if (!cancelled) setReminders(data); })
      .catch(() => { if (!cancelled) setReminders([]); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [taskId]);

  async function handleAddExact() {
    if (!remindAt) return;
    setSubmitting(true);
    try {
      const { data } = await apiClient.post<ReminderResponse>(`/tasks/${taskId}/reminders`, {
        remindAt: new Date(remindAt).toISOString(),
      });
      setReminders((prev) => [...prev, data].sort((a, b) => a.remindAt.localeCompare(b.remindAt)));
      setShowAdd(false);
      setRemindAt('');
    } catch { /* silently fail */ }
    finally { setSubmitting(false); }
  }

  async function handleAddOffset() {
    const preset = OFFSET_PRESETS[selectedPreset];
    const now = new Date();
    let remindDate: Date;
    if (preset.offsetType === 'MINUTES_BEFORE') {
      remindDate = new Date(now.getTime() + preset.offsetValue * 60 * 1000);
    } else if (preset.offsetType === 'HOURS_BEFORE') {
      remindDate = new Date(now.getTime() + preset.offsetValue * 3600 * 1000);
    } else {
      remindDate = new Date(now.getTime() + preset.offsetValue * 86400 * 1000);
    }
    setSubmitting(true);
    try {
      const { data } = await apiClient.post<ReminderResponse>(`/tasks/${taskId}/reminders`, {
        remindAt: remindDate.toISOString(),
        offsetType: preset.offsetType,
        offsetValue: preset.offsetValue,
      });
      setReminders((prev) => [...prev, data].sort((a, b) => a.remindAt.localeCompare(b.remindAt)));
      setShowAdd(false);
    } catch { /* silently fail */ }
    finally { setSubmitting(false); }
  }

  async function handleDelete(reminderId: string) {
    try {
      await apiClient.delete(`/reminders/${reminderId}`);
      setReminders((prev) => prev.filter((r) => r.id !== reminderId));
    } catch { /* silently fail */ }
  }

  if (loading) {
    return (
      <div className="mb-4">
        <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
          Reminders
        </label>
        <p className="text-xs text-stone-400">Loading...</p>
      </div>
    );
  }

  return (
    <div className="mb-4">
      <div className="mb-1 flex items-center justify-between">
        <label className="text-[11px] font-medium uppercase tracking-wide text-stone-400">
          Reminders
          {reminders.length > 0 && (
            <span className="ml-1 normal-case">({reminders.length})</span>
          )}
        </label>
        {!isCompleted && !showAdd && (
          <button
            type="button"
            onClick={() => setShowAdd(true)}
            className="rounded px-1.5 py-0.5 text-[11px] font-medium text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
          >
            + Add
          </button>
        )}
      </div>

      {/* Existing reminders */}
      {reminders.length > 0 && (
        <ul className="space-y-1">
          {reminders.map((r) => (
            <li key={r.id} className="group flex items-center gap-1.5 rounded px-1.5 py-1 hover:bg-stone-50">
              <span className="text-xs">🔔</span>
              <span className={`flex-1 text-xs ${r.isSent ? 'text-stone-400 line-through' : 'text-stone-600'}`}>
                {formatReminderDate(r.remindAt)}
                {r.offsetType && (
                  <span className="ml-1 text-stone-400">
                    ({formatOffset(r.offsetType, r.offsetValue)})
                  </span>
                )}
              </span>
              {r.isSent && <span className="text-[10px] text-stone-400">sent</span>}
              {!isCompleted && (
                <button
                  type="button"
                  onClick={() => handleDelete(r.id)}
                  className="hidden rounded p-0.5 text-stone-400 transition hover:bg-stone-200 hover:text-stone-600 group-hover:block"
                  title="Delete reminder"
                >
                  <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {reminders.length === 0 && !showAdd && (
        <p className="text-xs text-stone-400">No reminders set.</p>
      )}

      {/* Add reminder form */}
      {showAdd && (
        <div className="mt-2 rounded-md border border-stone-200 bg-stone-50 p-2">
          <div className="mb-2 flex gap-1">
            <button
              type="button"
              onClick={() => setAddMode('exact')}
              className={`rounded px-2 py-0.5 text-[10px] font-medium transition ${
                addMode === 'exact' ? 'bg-stone-900 text-white' : 'text-stone-500 hover:bg-stone-200'
              }`}
            >
              Exact time
            </button>
            <button
              type="button"
              onClick={() => setAddMode('offset')}
              className={`rounded px-2 py-0.5 text-[10px] font-medium transition ${
                addMode === 'offset' ? 'bg-stone-900 text-white' : 'text-stone-500 hover:bg-stone-200'
              }`}
            >
              Offset
            </button>
          </div>

          {addMode === 'exact' ? (
            <div className="flex items-center gap-1.5">
              <input
                type="datetime-local"
                value={remindAt}
                onChange={(e) => setRemindAt(e.target.value)}
                className="flex-1 rounded border border-stone-200 bg-white px-2 py-1 text-xs text-stone-700 outline-none focus:border-stone-400"
              />
              <button
                type="button"
                onClick={handleAddExact}
                disabled={submitting || !remindAt}
                className="rounded bg-stone-900 px-2 py-1 text-[10px] font-medium text-white transition hover:bg-stone-800 disabled:opacity-40"
              >
                Save
              </button>
            </div>
          ) : (
            <div className="flex items-center gap-1.5">
              <select
                value={selectedPreset}
                onChange={(e) => setSelectedPreset(Number(e.target.value))}
                className="flex-1 rounded border border-stone-200 bg-white px-2 py-1 text-xs text-stone-700 outline-none focus:border-stone-400"
              >
                {OFFSET_PRESETS.map((p, i) => (
                  <option key={i} value={i}>{p.label}</option>
                ))}
              </select>
              <button
                type="button"
                onClick={handleAddOffset}
                disabled={submitting}
                className="rounded bg-stone-900 px-2 py-1 text-[10px] font-medium text-white transition hover:bg-stone-800 disabled:opacity-40"
              >
                Save
              </button>
            </div>
          )}

          <button
            type="button"
            onClick={() => { setShowAdd(false); setRemindAt(''); }}
            className="mt-1.5 rounded px-1.5 py-0.5 text-[10px] text-stone-500 transition hover:bg-stone-200"
          >
            Cancel
          </button>
        </div>
      )}
    </div>
  );
}

function formatReminderDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString('en-US', {
    month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit',
  });
}

function formatOffset(offsetType: string, offsetValue: number | null): string {
  if (!offsetValue) return '';
  switch (offsetType) {
    case 'MINUTES_BEFORE': return `${offsetValue}m before`;
    case 'HOURS_BEFORE': return `${offsetValue}h before`;
    case 'DAYS_BEFORE': return `${offsetValue}d before`;
    default: return '';
  }
}

// --- Recurrence Section ---

type RecurrencePattern = { type: string; interval?: number; dayOfWeek?: string; time?: string };

const RECURRENCE_PRESETS: { label: string; rule: RecurrencePattern }[] = [
  { label: 'Daily', rule: { type: 'daily' } },
  { label: 'Weekly (Mon)', rule: { type: 'weekly', dayOfWeek: 'MONDAY' } },
  { label: 'Weekly (Fri)', rule: { type: 'weekly', dayOfWeek: 'FRIDAY' } },
  { label: 'Every 2 days', rule: { type: 'interval', interval: 2 } },
  { label: 'Every 7 days', rule: { type: 'interval', interval: 7 } },
  { label: 'Every 14 days', rule: { type: 'interval', interval: 14 } },
  { label: 'Every 30 days', rule: { type: 'interval', interval: 30 } },
];

function RecurrenceSection({
  task,
  onRecurrenceChanged,
}: {
  task: TaskResponse;
  onRecurrenceChanged: (rule: unknown) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [selectedPreset, setSelectedPreset] = useState(0);
  const [saving, setSaving] = useState(false);

  const currentRule = task.recurrenceRule as RecurrencePattern | null | undefined;
  const isRecurring = !!currentRule && typeof currentRule === 'object' && 'type' in currentRule;

  async function handleSetRecurrence() {
    const rule = RECURRENCE_PRESETS[selectedPreset].rule;
    setSaving(true);
    try {
      await apiClient.put(`/tasks/${task.id}`, { recurrenceRule: JSON.stringify(rule) });
      onRecurrenceChanged(rule);
      setEditing(false);
    } catch { /* silently fail */ }
    finally { setSaving(false); }
  }

  async function handleStopRecurrence() {
    setSaving(true);
    try {
      await apiClient.put(`/tasks/${task.id}`, { recurrenceRule: '' });
      onRecurrenceChanged(null);
      setEditing(false);
    } catch { /* silently fail */ }
    finally { setSaving(false); }
  }

  function describeRecurrence(rule: RecurrencePattern): string {
    switch (rule.type) {
      case 'daily': return 'Repeats daily';
      case 'weekly': return `Repeats weekly (${rule.dayOfWeek ? capitalize(rule.dayOfWeek) : 'every week'})`;
      case 'interval': return `Repeats every ${rule.interval} day${rule.interval !== 1 ? 's' : ''}`;
      default: return 'Repeats (custom)';
    }
  }

  return (
    <div className="mb-4">
      <div className="mb-1 flex items-center justify-between">
        <label className="text-[11px] font-medium uppercase tracking-wide text-stone-400">
          Recurrence
        </label>
        {!task.isCompleted && !editing && (
          <button
            type="button"
            onClick={() => setEditing(true)}
            className="rounded px-1.5 py-0.5 text-[11px] font-medium text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
          >
            {isRecurring ? 'Edit' : '+ Set'}
          </button>
        )}
      </div>

      {!editing && isRecurring && (
        <div className="flex items-center gap-1.5 rounded px-1.5 py-1 text-xs text-stone-600">
          <span>🔁</span>
          <span>{describeRecurrence(currentRule)}</span>
        </div>
      )}

      {!editing && !isRecurring && (
        <p className="text-xs text-stone-400">Not recurring.</p>
      )}

      {editing && (
        <div className="mt-1 rounded-md border border-stone-200 bg-stone-50 p-2">
          <select
            value={selectedPreset}
            onChange={(e) => setSelectedPreset(Number(e.target.value))}
            className="mb-2 w-full rounded border border-stone-200 bg-white px-2 py-1 text-xs text-stone-700 outline-none focus:border-stone-400"
          >
            {RECURRENCE_PRESETS.map((p, i) => (
              <option key={i} value={i}>{p.label}</option>
            ))}
          </select>
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={handleSetRecurrence}
              disabled={saving}
              className="rounded bg-stone-900 px-2 py-1 text-[10px] font-medium text-white transition hover:bg-stone-800 disabled:opacity-40"
            >
              {isRecurring ? 'Update' : 'Enable'}
            </button>
            {isRecurring && (
              <button
                type="button"
                onClick={handleStopRecurrence}
                disabled={saving}
                className="rounded border border-red-200 px-2 py-1 text-[10px] font-medium text-red-600 transition hover:bg-red-50 disabled:opacity-40"
              >
                Stop
              </button>
            )}
            <button
              type="button"
              onClick={() => setEditing(false)}
              className="rounded px-1.5 py-0.5 text-[10px] text-stone-500 transition hover:bg-stone-200"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

function capitalize(s: string): string {
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase();
}
