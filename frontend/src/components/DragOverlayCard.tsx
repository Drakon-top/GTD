import { CalendarDays } from 'lucide-react';
import type { TaskResponse } from '../types';
import type { ThemeColors } from '../utils/themes';

interface DragOverlayCardProps {
  task: TaskResponse;
  theme: ThemeColors;
}

export default function DragOverlayCard({ task, theme }: DragOverlayCardProps) {
  return (
    <div className={`w-72 ${theme.borderRadiusLg} ${theme.panelBg} ${theme.dropTargetRing} ${theme.spacing} ${theme.shadowLg}`}>
      <p className={`truncate text-sm font-medium ${theme.taskText}`}>{task.title}</p>
      {task.dueDate && (
        <p className={`mt-1 flex items-center gap-1 text-xs ${theme.taskSubtext}`}>
          <CalendarDays className="h-3 w-3" /> {new Date(task.dueDate).toLocaleDateString()}
        </p>
      )}
    </div>
  );
}
