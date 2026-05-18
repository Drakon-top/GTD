import { useEffect, useState } from 'react';
import apiClient from '../api/client';
import type { GtdList, TaskResponse } from '../types';
import { gtdListLabel } from '../utils/gtdLabels';

interface TaskDetailPanelProps {
  taskId: string;
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

export default function TaskDetailPanel({ taskId, onClose, onTaskChanged }: TaskDetailPanelProps) {
  const [task, setTask] = useState<TaskResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [editingTitle, setEditingTitle] = useState(false);
  const [titleDraft, setTitleDraft] = useState('');
  const [notesDraft, setNotesDraft] = useState('');
  const [savingNotes, setSavingNotes] = useState(false);

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
      onTaskChanged();
      setTask((prev) => prev ? { ...prev, title: titleDraft.trim() } : prev);
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
      onTaskChanged();
      setTask((prev) => prev ? { ...prev, notes: notesDraft || null } : prev);
    } catch {
      setNotesDraft(task.notes ?? '');
    } finally {
      setSavingNotes(false);
    }
  }

  async function handleMove(gtdList: GtdList) {
    if (!task) return;
    try {
      await apiClient.patch(`/tasks/${task.id}/move`, { gtdList });
      onTaskChanged();
      setTask((prev) => prev ? { ...prev, gtdList } : prev);
    } catch {
      // silently fail
    }
  }

  async function handleDelete() {
    if (!task) return;
    try {
      await apiClient.delete(`/tasks/${task.id}`);
      onTaskChanged();
      onClose();
    } catch {
      // silently fail
    }
  }

  async function handleComplete() {
    if (!task) return;
    try {
      await apiClient.patch(`/tasks/${task.id}/complete`);
      onTaskChanged();
      onClose();
    } catch {
      // silently fail
    }
  }

  if (loading) {
    return (
      <div className="flex h-full w-80 shrink-0 items-center justify-center border-l border-stone-200 bg-white">
        <div className="h-5 w-5 animate-spin rounded-full border-2 border-stone-300 border-t-stone-800" />
      </div>
    );
  }

  if (!task) return null;

  return (
    <div className="flex h-full w-80 shrink-0 flex-col border-l border-stone-200 bg-white">
      <div className="flex items-center justify-between border-b border-stone-200 px-4 py-3">
        <span className="text-xs font-medium uppercase tracking-wide text-stone-400">Details</span>
        <button
          type="button"
          onClick={onClose}
          className="rounded-md p-1 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto px-4 py-4">
        {editingTitle ? (
          <input
            autoFocus
            value={titleDraft}
            onChange={(e) => setTitleDraft(e.target.value)}
            onBlur={saveTitle}
            onKeyDown={(e) => { if (e.key === 'Enter') saveTitle(); if (e.key === 'Escape') { setTitleDraft(task.title); setEditingTitle(false); } }}
            className="mb-3 w-full rounded border border-stone-300 px-2 py-1 text-sm font-semibold text-stone-900 outline-none focus:border-stone-500"
          />
        ) : (
          <h3
            onClick={() => setEditingTitle(true)}
            className="mb-3 cursor-pointer text-sm font-semibold text-stone-900 hover:bg-stone-50 rounded px-1 -mx-1"
          >
            {task.title}
          </h3>
        )}

        <div className="mb-4">
          <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
            GTD List
          </label>
          <select
            value={task.gtdList}
            onChange={(e) => handleMove(e.target.value as GtdList)}
            className="w-full rounded-md border border-stone-200 bg-white px-2 py-1.5 text-sm text-stone-700 outline-none focus:border-stone-400"
          >
            {GTD_OPTIONS.map((g) => (
              <option key={g} value={g}>{gtdListLabel(g)}</option>
            ))}
          </select>
        </div>

        {task.dueDate && (
          <div className="mb-4">
            <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
              Due Date
            </label>
            <p className="text-sm text-stone-700">
              {new Date(task.dueDate).toLocaleDateString('en-US', {
                weekday: 'short', year: 'numeric', month: 'short', day: 'numeric',
              })}
            </p>
          </div>
        )}

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
            className="w-full resize-none rounded-md border border-stone-200 px-2 py-1.5 text-sm text-stone-700 outline-none placeholder:text-stone-400 focus:border-stone-400"
          />
          {savingNotes && <p className="mt-0.5 text-[10px] text-stone-400">Saving...</p>}
        </div>

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

        {task.subtasks && task.subtasks.length > 0 && (
          <div className="mb-4">
            <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
              Subtasks ({task.subtasks.filter((s) => s.isCompleted).length}/{task.subtasks.length})
            </label>
            <ul className="space-y-1">
              {task.subtasks.map((sub) => (
                <li key={sub.id} className="flex items-center gap-2 rounded px-1 py-0.5 text-sm">
                  <span className={`h-3 w-3 shrink-0 rounded-sm border ${
                    sub.isCompleted ? 'border-stone-300 bg-stone-200' : 'border-stone-300'
                  }`} />
                  <span className={sub.isCompleted ? 'text-stone-400 line-through' : 'text-stone-700'}>
                    {sub.title}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {task.isRecurring && (
          <div className="mb-4">
            <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
              Recurring
            </label>
            <p className="text-sm text-stone-600">🔁 This task repeats</p>
          </div>
        )}

        <div className="mb-2 text-[10px] text-stone-400">
          Created {new Date(task.createdAt).toLocaleDateString('en-US', {
            month: 'short', day: 'numeric', year: 'numeric',
          })}
          {' · '}Version {task.version}
        </div>
      </div>

      <div className="border-t border-stone-200 px-4 py-3">
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
            onClick={handleDelete}
            className="rounded-md border border-red-200 px-3 py-1.5 text-sm font-medium text-red-600 transition hover:bg-red-50"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}
