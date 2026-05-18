import { useRef, useState } from 'react';
import { useDraggable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import apiClient from '../api/client';
import type { GtdList, TaskResponse } from '../types';
import type { ThemeColors } from '../utils/themes';

const GTD_MOVE_TARGETS: { key: GtdList; label: string; icon: string }[] = [
  { key: 'INBOX', label: 'Inbox', icon: '📥' },
  { key: 'NEXT_ACTIONS', label: 'Next Actions', icon: '⚡' },
  { key: 'PROJECTS', label: 'Projects', icon: '📁' },
  { key: 'WAITING_FOR', label: 'Waiting For', icon: '⏳' },
  { key: 'SOMEDAY_MAYBE', label: 'Someday / Maybe', icon: '💭' },
  { key: 'REFERENCE', label: 'Reference', icon: '📎' },
  { key: 'CALENDAR', label: 'Calendar', icon: '📅' },
  { key: 'DONE', label: 'Done', icon: '✅' },
];

interface TaskListProps {
  tasks: TaskResponse[];
  sectionLabel: string;
  contextId: string;
  onTaskClick: (task: TaskResponse) => void;
  onTasksChanged: () => void;
  selectedTaskId: string | null;
  theme: ThemeColors;
}

function DraggableTaskItem({
  task,
  isSelected,
  onTaskClick,
  onToggleComplete,
  onContextMenu,
  formatDueDate,
  dueDateColor,
  theme,
}: {
  task: TaskResponse;
  isSelected: boolean;
  onTaskClick: () => void;
  onToggleComplete: (e: React.MouseEvent) => void;
  onContextMenu: (e: React.MouseEvent) => void;
  formatDueDate: (iso: string | null) => string | null;
  dueDateColor: (iso: string | null) => string;
  theme: ThemeColors;
}) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `task:${task.id}`,
    data: { task },
  });

  const style = transform
    ? { transform: CSS.Translate.toString(transform), opacity: isDragging ? 0.5 : 1 }
    : undefined;

  const due = formatDueDate(task.dueDate);

  return (
    <li ref={setNodeRef} style={style} {...listeners} {...attributes}>
      <button
        type="button"
        onClick={onTaskClick}
        onContextMenu={onContextMenu}
        className={`flex w-full items-start gap-3 px-5 py-3 text-left transition ${
          isDragging
            ? `${theme.dropTargetBg} shadow-md ${theme.dropTargetRing}`
            : isSelected
              ? theme.taskSelected
              : theme.taskHover
        }`}
      >
        <span
          role="checkbox"
          aria-checked={task.isCompleted}
          tabIndex={0}
          onClick={onToggleComplete}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              onToggleComplete(e as unknown as React.MouseEvent);
            }
          }}
          onPointerDown={(e) => e.stopPropagation()}
          className={`mt-0.5 flex h-[18px] w-[18px] shrink-0 items-center justify-center rounded-full border transition ${
            task.isCompleted ? theme.checkboxChecked : theme.checkbox
          }`}
        >
          {task.isCompleted && (
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          )}
        </span>

        <div className="min-w-0 flex-1">
          <p className={`text-sm leading-snug ${
            task.isCompleted ? `${theme.taskCompletedText} line-through` : theme.taskText
          }`}>
            {task.title}
          </p>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            {task.subtaskCount != null && task.subtaskCount > 0 && (
              <span className={`text-xs ${theme.taskSubtext}`}>
                {task.completedSubtaskCount ?? 0}/{task.subtaskCount} subtasks
              </span>
            )}
            {task.progress != null && (
              <div className="flex items-center gap-1">
                <div className={`h-1 w-12 overflow-hidden rounded-full ${theme.progressBg}`}>
                  <div
                    className={`h-full rounded-full ${theme.progressFill} transition-all`}
                    style={{ width: `${task.progress}%` }}
                  />
                </div>
                <span className={`text-[10px] ${theme.taskSubtext}`}>{task.progress}%</span>
              </div>
            )}
            {due && (
              <span className={`text-xs ${dueDateColor(task.dueDate)}`}>
                📅 {due}
              </span>
            )}
            {task.isRecurring && (
              <span className={`text-xs ${theme.taskSubtext}`} title="Recurring">🔁</span>
            )}
          </div>
        </div>

        <span
          role="button"
          tabIndex={0}
          onClick={onContextMenu}
          onKeyDown={(e) => {
            if (e.key === 'Enter') onContextMenu(e as unknown as React.MouseEvent);
          }}
          onPointerDown={(e) => e.stopPropagation()}
          className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-md ${theme.taskSubtext} opacity-0 transition hover:opacity-100 [li:hover_&]:opacity-100`}
        >
          <svg className="h-4 w-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M10 6a2 2 0 110-4 2 2 0 010 4zm0 6a2 2 0 110-4 2 2 0 010 4zm0 6a2 2 0 110-4 2 2 0 010 4z" />
          </svg>
        </span>
      </button>
    </li>
  );
}

export default function TaskList({
  tasks,
  sectionLabel,
  contextId,
  onTaskClick,
  onTasksChanged,
  selectedTaskId,
  theme,
}: TaskListProps) {
  const [newTitle, setNewTitle] = useState('');
  const [creating, setCreating] = useState(false);
  const [contextMenu, setContextMenu] = useState<{ taskId: string; x: number; y: number } | null>(null);
  const [moveSubmenuOpen, setMoveSubmenuOpen] = useState(false);
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
    setMoveSubmenuOpen(false);
    try {
      await apiClient.delete(`/tasks/${taskId}`);
      onTasksChanged();
    } catch { /* silently fail */ }
  }

  async function handleMoveTask(taskId: string, gtdList: GtdList) {
    setContextMenu(null);
    setMoveSubmenuOpen(false);
    try {
      await apiClient.patch(`/tasks/${taskId}/move`, { gtdList });
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
    setMoveSubmenuOpen(false);
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

  const contextTask = contextMenu ? tasks.find((t) => t.id === contextMenu.taskId) : null;

  return (
    <div
      ref={listRef}
      className="relative flex h-full flex-col"
      onClick={() => { setContextMenu(null); setMoveSubmenuOpen(false); }}
    >
      <div className={`flex items-center justify-between border-b ${theme.listHeaderBorder} px-5 py-3`}>
        <h2 className={`text-base font-semibold ${theme.listHeaderText}`}>{sectionLabel}</h2>
        <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${theme.listCountBadge}`}>
          {tasks.length}
        </span>
      </div>

      <div className="flex-1 overflow-y-auto">
        {tasks.length === 0 && (
          <div className="flex flex-col items-center justify-center px-5 py-16 text-center">
            <div className={`mb-2 text-3xl ${theme.emptyIcon}`}>📋</div>
            <p className={`text-sm ${theme.emptyText}`}>No tasks here yet</p>
            <p className={`mt-1 text-xs ${theme.emptyText} opacity-70`}>Add one below or drag tasks here</p>
          </div>
        )}

        <ul className={`divide-y ${theme.taskDivider}`}>
          {tasks.map((task) => (
            <DraggableTaskItem
              key={task.id}
              task={task}
              isSelected={task.id === selectedTaskId}
              onTaskClick={() => onTaskClick(task)}
              onToggleComplete={(e) => handleToggleComplete(task, e)}
              onContextMenu={(e) => handleContextMenu(e, task)}
              formatDueDate={formatDueDate}
              dueDateColor={dueDateColor}
              theme={theme}
            />
          ))}
        </ul>
      </div>

      {contextMenu && contextTask && (
        <div
          className="absolute z-50 w-44 rounded-lg border border-stone-200 bg-white py-1 shadow-lg"
          style={{ left: contextMenu.x, top: contextMenu.y }}
          onClick={(e) => e.stopPropagation()}
        >
          <button
            type="button"
            onClick={() => {
              onTaskClick(contextTask);
              setContextMenu(null);
            }}
            className="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm text-stone-700 hover:bg-stone-50"
          >
            <span className="text-xs">✏️</span> Edit
          </button>
          {!contextTask.isCompleted && (
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

          {!contextTask.isCompleted && (
            <div className="relative">
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); setMoveSubmenuOpen(!moveSubmenuOpen); }}
                className="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm text-stone-700 hover:bg-stone-50"
              >
                <span className="text-xs">➡️</span>
                <span className="flex-1">Move to...</span>
                <svg className="h-3 w-3 text-stone-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                </svg>
              </button>

              {moveSubmenuOpen && (
                <div className="absolute left-full top-0 ml-1 w-44 rounded-lg border border-stone-200 bg-white py-1 shadow-lg">
                  {GTD_MOVE_TARGETS
                    .filter(({ key }) => key !== contextTask.gtdList)
                    .map(({ key, label, icon }) => (
                      <button
                        key={key}
                        type="button"
                        onClick={() => handleMoveTask(contextMenu.taskId, key)}
                        className="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm text-stone-700 hover:bg-stone-50"
                      >
                        <span className="text-xs">{icon}</span> {label}
                      </button>
                    ))}
                </div>
              )}
            </div>
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

      <form
        onSubmit={handleAddTask}
        className={`border-t ${theme.listHeaderBorder} px-5 py-3`}
      >
        <div className="flex items-center gap-2">
          <input
            type="text"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            placeholder="Add a task..."
            disabled={creating}
            className={`flex-1 rounded-md border ${theme.inputBorder} px-3 py-1.5 text-sm ${theme.inputText} ${theme.inputBg} outline-none ${theme.inputPlaceholder} ${theme.inputFocus}`}
          />
          <button
            type="submit"
            disabled={creating || !newTitle.trim()}
            className={`rounded-md ${theme.btnPrimary} px-3 py-1.5 text-sm font-medium ${theme.btnPrimaryText} transition ${theme.btnPrimaryHover} disabled:opacity-40`}
          >
            {creating ? '...' : 'Add'}
          </button>
        </div>
      </form>
    </div>
  );
}
