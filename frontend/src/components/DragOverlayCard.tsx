import { CalendarDays } from 'lucide-react';
import type { TaskResponse } from '../types';

interface DragOverlayCardProps {
  task: TaskResponse;
}

export default function DragOverlayCard({ task }: DragOverlayCardProps) {
  return (
    <div className="w-72 rounded-lg border border-blue-200 bg-white px-4 py-3 shadow-xl ring-1 ring-blue-100">
      <p className="truncate text-sm font-medium text-stone-800">{task.title}</p>
      {task.dueDate && (
        <p className="mt-1 flex items-center gap-1 text-xs text-stone-400">
          <CalendarDays className="h-3 w-3" /> {new Date(task.dueDate).toLocaleDateString()}
        </p>
      )}
    </div>
  );
}
