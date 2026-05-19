import { useRef, useState } from 'react';
import { useDraggable } from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import {
  Inbox, Zap, FolderOpen, Clock, Lightbulb, BookOpen, Calendar,
  CheckCircle, Check, MoreVertical, ChevronRight, ClipboardList,
  CalendarDays, Repeat, Pencil, ArrowRight, Trash2,
} from 'lucide-react';
import type { ComponentType } from 'react';
import apiClient from '../api/client';
import type { GtdList, TaskResponse } from '../types';
import type { ThemeColors } from '../utils/themes';

const GTD_MOVE_TARGETS: { key: GtdList; label: string; Icon: ComponentType<{ className?: string }> }[] = [
  { key: 'INBOX', label: 'Inbox', Icon: Inbox },
  { key: 'NEXT_ACTIONS', label: 'Next Actions', Icon: Zap },
  { key: 'PROJECTS', label: 'Projects', Icon: FolderOpen },
  { key: 'WAITING_FOR', label: 'Waiting For', Icon: Clock },
  { key: 'SOMEDAY_MAYBE', label: 'Someday / Maybe', Icon: Lightbulb },
  { key: 'REFERENCE', label: 'Reference', Icon: BookOpen },
  { key: 'CALENDAR', label: 'Calendar', Icon: Calendar },
  { key: 'DONE', label: 'Done', Icon: CheckCircle },
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
  index,
}: {
  task: TaskResponse;
  isSelected: boolean;
  onTaskClick: () => void;
  onToggleComplete: (e: React.MouseEvent) => void;
  onContextMenu: (e: React.MouseEvent) => void;
  formatDueDate: (iso: string | null) => string | null;
  dueDateColor: (iso: string | null) => string;
  theme: ThemeColors;
  index: number;
}) {
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `task:${task.id}`,
    data: { task },
  });

  const style = transform
    ? { transform: CSS.Translate.toString(transform), opacity: isDragging ? 0.5 : 1 }
    : undefined;

  const due = formatDueDate(task.dueDate);

  const hoverIndicatorClass = theme.taskHoverIndicator || '';
  const floatClass = theme.floatingShadow || '';
  const stampClass = task.isCompleted && theme.taskCompletedStamp ? 'theme-formal-stamp' : '';
  const underlineClass = isSelected && theme.taskActiveUnderline ? theme.taskActiveUnderline : '';

  return (
    <li ref={setNodeRef} style={style} {...listeners} {...attributes}>
      <button
        type="button"
        onClick={onTaskClick}
        onContextMenu={onContextMenu}
        className={`flex w-full items-start gap-3 ${theme.spacing} text-left transition ${theme.transitionSpeed} ${hoverIndicatorClass} ${floatClass} ${stampClass} ${underlineClass} ${
          isDragging
            ? `${theme.dropTargetBg} ${theme.shadow} ${theme.dropTargetRing}`
            : isSelected
              ? theme.taskSelected
              : theme.taskHover
        }`}
      >
        {theme.neonDotIndicator && !task.isCompleted && (
          <span className="theme-dark-neon-dot mt-1.5 shrink-0" />
        )}
        {theme.taskNumbering && (
          <span className="mt-0.5 w-6 shrink-0 text-right text-[10px] font-light text-slate-300 font-mono">
            {String(index + 1).padStart(2, '0')}.
          </span>
        )}
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
          className={`mt-0.5 flex ${theme.checkboxSize} shrink-0 items-center justify-center ${theme.checkboxRadius} border transition ${theme.transitionSpeed} ${theme.checkboxExtra} ${
            task.isCompleted ? theme.checkboxChecked : theme.checkbox
          }`}
        >
          {task.isCompleted && <Check className="h-3 w-3" />}
        </span>

        <div className="min-w-0 flex-1">
          <p className={`text-sm leading-snug ${theme.fontFamily} ${
            task.isCompleted ? `${theme.taskCompletedText} line-through` : theme.taskText
          }`}>
            {theme.dropCapProject && task.subtaskCount != null && task.subtaskCount > 0 && !task.isCompleted ? (
              <><span className="font-serif text-lg font-semibold leading-none">{task.title.charAt(0)}</span>{task.title.slice(1)}</>
            ) : task.title}
          </p>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            {task.subtaskCount != null && task.subtaskCount > 0 && (
              <span className={`text-xs ${theme.taskSubtext}`}>
                {task.completedSubtaskCount ?? 0}/{task.subtaskCount} subtasks
              </span>
            )}
            {task.progress != null && (
              <div className="flex items-center gap-1">
                <div className={`${theme.progressHeightSm} w-12 overflow-hidden ${theme.progressRadius} ${theme.progressBg}`}>
                  <div
                    className={`h-full ${theme.progressRadius} ${theme.progressFill} ${theme.progressExtra} transition-all ${theme.transitionSpeed}`}
                    style={{ width: `${task.progress}%` }}
                  />
                </div>
                <span className={`text-[10px] ${theme.taskSubtext}`}>{task.progress}%</span>
              </div>
            )}
            {due && (
              <span className={`inline-flex items-center gap-0.5 text-xs ${dueDateColor(task.dueDate)}`}>
                <CalendarDays className="h-3 w-3" /> {due}
              </span>
            )}
            {task.isRecurring && (
              <span className={`text-xs ${theme.taskSubtext}`} title="Recurring">
                <Repeat className="h-3 w-3" />
              </span>
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
          className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center ${theme.borderRadius} ${theme.taskSubtext} opacity-0 transition ${theme.transitionSpeed} hover:opacity-100 [li:hover_&]:opacity-100`}
        >
          <MoreVertical className="h-4 w-4" />
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
      <div className={`flex items-center justify-between border-b ${theme.listHeaderBorder} ${theme.spacing}`}>
        <h2 className={`text-base font-semibold ${theme.headerFont} ${theme.listHeaderText} ${theme.headerAccent}`}>{sectionLabel}</h2>
        <span className={`${theme.borderRadius} px-2 py-0.5 text-xs font-medium ${theme.listCountBadge} ${theme.badgeFont} ${theme.blobBadge ? 'theme-nature-blob' : ''}`}>
          {tasks.length}
        </span>
      </div>

      <div className="flex-1 overflow-y-auto">
        {tasks.length === 0 && (
          <div className={`flex flex-col items-center justify-center ${theme.spacing} py-16 text-center`}>
            <ClipboardList className={`mb-2 h-8 w-8 ${theme.emptyIcon}`} />
            <p className={`text-sm ${theme.emptyText}`}>No tasks here yet</p>
            <p className={`mt-1 text-xs ${theme.emptyText} opacity-70`}>Add one below or drag tasks here</p>
          </div>
        )}

        <ul className={`divide-y ${theme.taskDivider}`}>
          {tasks.map((task, idx) => (
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
              index={idx}
            />
          ))}
        </ul>
      </div>

      {contextMenu && contextTask && (
        <div
          className={`absolute z-50 w-44 ${theme.borderRadiusLg} ${theme.decorativeBorder} ${theme.inputBorder} ${theme.panelBg} py-1 ${theme.shadowLg}`}
          style={{ left: contextMenu.x, top: contextMenu.y }}
          onClick={(e) => e.stopPropagation()}
        >
          <button
            type="button"
            onClick={() => {
              onTaskClick(contextTask);
              setContextMenu(null);
            }}
            className={`flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm ${theme.taskText} transition ${theme.transitionSpeed} ${theme.taskHover}`}
          >
            <Pencil className="h-3.5 w-3.5" /> Edit
          </button>
          {!contextTask.isCompleted && (
            <button
              type="button"
              onClick={() => {
                apiClient.patch(`/tasks/${contextMenu.taskId}/complete`).then(onTasksChanged).catch(() => {});
                setContextMenu(null);
              }}
              className={`flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm ${theme.taskText} transition ${theme.transitionSpeed} ${theme.taskHover}`}
            >
              <CheckCircle className="h-3.5 w-3.5" /> Complete
            </button>
          )}

          {!contextTask.isCompleted && (
            <div className="relative">
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); setMoveSubmenuOpen(!moveSubmenuOpen); }}
                className={`flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm ${theme.taskText} transition ${theme.transitionSpeed} ${theme.taskHover}`}
              >
                <ArrowRight className="h-3.5 w-3.5" />
                <span className="flex-1">Move to...</span>
                <ChevronRight className={`h-3 w-3 ${theme.taskSubtext}`} />
              </button>

              {moveSubmenuOpen && (
                <div className={`absolute left-full top-0 ml-1 w-44 ${theme.borderRadiusLg} ${theme.decorativeBorder} ${theme.inputBorder} ${theme.panelBg} py-1 ${theme.shadowLg}`}>
                  {GTD_MOVE_TARGETS
                    .filter(({ key }) => key !== contextTask.gtdList)
                    .map(({ key, label, Icon }) => (
                      <button
                        key={key}
                        type="button"
                        onClick={() => handleMoveTask(contextMenu.taskId, key)}
                        className={`flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm ${theme.taskText} transition ${theme.transitionSpeed} ${theme.taskHover}`}
                      >
                        <Icon className="h-3.5 w-3.5" /> {label}
                      </button>
                    ))}
                </div>
              )}
            </div>
          )}

          <div className={`my-1 border-t ${theme.sidebarDivider}`} />
          <button
            type="button"
            onClick={() => handleDeleteTask(contextMenu.taskId)}
            className={`flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm text-red-600 transition ${theme.transitionSpeed} hover:bg-red-50`}
          >
            <Trash2 className="h-3.5 w-3.5" /> Delete
          </button>
        </div>
      )}

      <form
        onSubmit={handleAddTask}
        className={`border-t ${theme.listHeaderBorder} ${theme.spacing}`}
      >
        <div className="flex items-center gap-2">
          <input
            type="text"
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            placeholder="Add a task..."
            disabled={creating}
            className={`flex-1 ${theme.borderRadius} border ${theme.inputBorder} px-3 py-1.5 text-sm ${theme.inputText} ${theme.inputBg} outline-none ${theme.inputPlaceholder} ${theme.inputFocus}`}
          />
          <button
            type="submit"
            disabled={creating || !newTitle.trim()}
            className={`${theme.borderRadius} ${theme.btnPrimary} px-3 py-1.5 text-sm font-medium ${theme.btnPrimaryText} transition ${theme.btnPrimaryHover} disabled:opacity-40 ${theme.addBtnExtra}`}
          >
            {creating ? '...' : 'Add'}
          </button>
        </div>
      </form>
    </div>
  );
}
