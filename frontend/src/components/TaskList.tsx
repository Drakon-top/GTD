import { useRef, useState } from 'react';
import apiClient from '../api/client';
import type { TaskResponse } from '../types';

interface TaskListProps {
  tasks: TaskResponse[];
  sectionLabel: string;
  contextId: string;
  onTaskClick: (task: TaskResponse) => void;
  onTasksChanged: () => void;
  selectedTaskId: string | null;
}

export default function TaskList({
  tasks,
  sectionLabel,
  contextId,
  onTaskClick,
  onTasksChanged,
  selectedTaskId,
}: TaskListProps) {
  const [newTitle, setNewTitle] = useState('');
  const [creating, setCreating] = useState(false);
  const [contextMenu, setContextMenu] = useState<{ taskId: string; x: number; y: number } | null>(null);
  const listRef = useRef<HTMLDivElement>(null);

  async function handleAddTask(e: React.FormEvent) {
    e.preventDefault();
    const title = newTitle.trim();
    if (!title) return;

    setCreating(true);
    try {
      await apiClient.post(`/contexts/${contextId}/tasks`, { title });
      setNewTitle('');
      onTasksChanged();
    } catch { /* silently fail */ }
    finally {
      setCreating(false);
    }
  }

  async function handleToggleComplete(task: TaskResponse, e: React.MouseEvent) {
    e.stopPropagation();
    if (task.isCompleted) return;
    try {
      await apiClient.patch(`/tasks/${task.id}/complete`);
      onTasksChanged();
    } catch { /* silently fail */ }
  }

  async function handleDeleteTask(taskId: string) {
    setContextMenu(null);
    try {
      await apiClient.delete(`/tasks/${taskId}`);
      onTasksChanged();
    } catch { /* silently fail */ }
  }

  function handleContextMenu(e: React.MouseEvent, task: TaskResponse) {
    e.preventDefault();
    e.stopPropagation();
    const rect = listRef.current?.getBoundingClientRect();
    const x = e.clientX - (rect?.left ?? 0);
    const y = e.clientY - (rect?.top ?? 0);
    setContextMenu({ taskId: task.id, x, y });
  }

  function formatDueDate(iso: string | null): string | null {
    if (!iso) return null;
    const d = new Date(iso);
    const now = new Date();
    const diffMs = d.getTime() - now.getTime();
    const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays < 0) return 'Overdue';
    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Tomorrow';
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
  }

  function dueDateColor(iso: string | null): string {
    if (!iso) return '';
    const d = new Date(iso);
    const now = new Date();
    const diffMs = d.getTime() - now.getTime();
    const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays < 0) return 'text-red-500';
    if (diffDays <= 1) return 'text-amber-500';
    return 'text-stone-400';
  }

  return (
    <div
      ref={listRef}
      className="relative flex h-full flex-col"
      onClick={() => setContextMenu(null)}
    >
      {/* Header */}
      <div className="flex items-center justify-between border-b border-stone-200 px-5 py-3">
        <h2 className="text-base font-semibold text-stone-900">{sectionLabel}</h2>
        <span className="rounded-full bg-stone-100 px-2 py-0.5 text-xs font-medium text-stone-500">
          {tasks.length}
        </span>
      </div>

      {/* Task list */}
      <div className="flex-1 overflow-y-auto">
        {tasks.length === 0 && (
          <div className="flex flex-col items-center justify-center px-5 py-16 text-center">
            <div className="mb-2 text-3xl opacity-30">📋</div>
            <p className="text-sm text-stone-400">No tasks here yet</p>
            <p className="mt-1 text-xs text-stone-300">Add one below to get started</p>
          </div>
        )}

        <ul className="divide-y divide-stone-100">
          {tasks.map((task) => {
            const due = formatDueDate(task.dueDate);
            const isSelected = task.id === selectedTaskId;
            return (
              <li key={task.id}>
                <button
                  type="button"
                  onClick={() => onTaskClick(task)}
                  onContextMenu={(e) => handleContextMenu(e, task)}
                  className={`flex w-full items-start gap-3 px-5 py-3 text-left transition ${
                    isSelected ? 'bg-stone-50' : 'hover:bg-stone-50/50'
                  }`}
                >
                  {/* Checkbox */}
                  <span
                    role="checkbox"
                    aria-checked={task.isCompleted}
                    tabIndex={0}
                    onClick={(e) => handleToggleComplete(task, e)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        handleToggleComplete(task, e as unknown as React.MouseEvent);
                      }
                    }}
                    className={`mt-0.5 flex h-[18px] w-[18px] shrink-0 items-center justify-center rounded-full border transition ${
                      task.isCompleted
                        ? 'border-stone-300 bg-stone-200 text-stone-500'
                        : 'border-stone-300 hover:border-stone-500'
                    }`}
                  >
                    {task.isCompleted && (
                      <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                    )}
                  </span>

                  {/* Task content */}
                  <div className="min-w-0 flex-1">
                    <p className={`text-sm leading-snug ${
                      task.isCompleted ? 'text-stone-400 line-through' : 'text-stone-800'
                    }`}>
                      {task.title}
                    </p>
                    <div className="mt-1 flex flex-wrap items-center gap-2">
                      {task.subtasks && task.subtasks.length > 0 && (
                        <span className="text-xs text-stone-400">
                          {task.subtasks.filter((s: TaskResponse) => s.isCompleted).length}/{task.subtasks.length} subtasks
                        </span>
                      )}
                      {task.progress != null && (
                        <div className="flex items-center gap-1">
                          <div className="h-1 w-12 overflow-hidden rounded-full bg-stone-200">
                            <div
                              className="h-full rounded-full bg-stone-500 transition-all"
                              style={{ width: `${task.progress}%` }}
                            />
                          </div>
                          <span className="text-[10px] text-stone-400">{task.progress}%</span>
                        </div>
                      )}
                      {due && (
                        <span className={`text-xs ${dueDateColor(task.dueDate)}`}>
                          📅 {due}
                        </span>
                      )}
                      {task.isRecurring && (
                        <span className="text-xs text-stone-400" title="Recurring">🔁</span>
                      )}
                    </div>
                  </div>

                  {/* More button */}
                  <span
                    role="button"
                    tabIndex={0}
                    onClick={(e) => handleContextMenu(e, task)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') handleContextMenu(e as unknown as React.MouseEvent, task);
                    }}
                    className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-md text-stone-300 opacity-0 transition group-hover:opacity-100 hover:bg-stone-100 hover:text-stone-500 [li:hover_&]:opacity-100"
                  >
                    <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
                      <path d="M10 6a2 2 0 110-4 2 2 0 010 4zm0 6a2 2 0 110-4 2 2 0 010 4zm0 6a2 2 0 110-4 2 2 0 010 4z" />
                    </svg>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      </div>

      {/* Context menu */}
      {contextMenu && (
        <div
          className="absolute z-50 w-40 rounded-lg border border-stone-200 bg-white py-1 shadow-lg"
          style={{ left: contextMenu.x, top: contextMenu.y }}
        >
          <button
            type="button"
            onClick={() => {
              const task = tasks.find((t) => t.id === contextMenu.taskId);
              if (task) onTaskClick(task);
              setContextMenu(null);
            }}
            className="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm text-stone-700 hover:bg-stone-50"
          >
            <span className="text-xs">✏️</span> Edit
          </button>
          {!tasks.find((t) => t.id === contextMenu.taskId)?.isCompleted && (
            <button
              type="button"
              onClick={() => {
                apiClient.patch(`/tasks/${contextMenu.taskId}/complete`).then(onTasksChanged).catch(() => {});
                setContextMenu(null);
              }}
              className="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm text-stone-700 hover:bg-stone-50"
            >
              <span className="text-xs">✅</span> Complete
            </button>
          )}
          <div className="my-1 border-t border-stone-100" />
          <button
            type="button"
            onClick={() => handleDeleteTask(contextMenu.taskId)}
            className="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm text-red-600 hover:bg-red-50"
          >
            <span className="text-xs">🗑️</span> Delete
          </button>
        </div>
      )}

      {/* Add task form */}
      <form
        onSubmit={handleAddTask}
        className="border-t border-stone-200 px-5 py-3"
      >
        <div className="flex items-center gap-2">
          <input
            type="text"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            placeholder="Add a task..."
            disabled={creating}
            className="flex-1 rounded-md border border-stone-200 px-3 py-1.5 text-sm text-stone-800 outline-none placeholder:text-stone-400 focus:border-stone-400 focus:ring-1 focus:ring-stone-400/30"
          />
          <button
            type="submit"
            disabled={creating || !newTitle.trim()}
            className="rounded-md bg-stone-900 px-3 py-1.5 text-sm font-medium text-white transition hover:bg-stone-800 disabled:opacity-40"
          >
            {creating ? '...' : 'Add'}
          </button>
        </div>
      </form>
    </div>
  );
}
