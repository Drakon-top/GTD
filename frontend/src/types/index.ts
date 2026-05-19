export type GtdList =
  | 'INBOX'
  | 'NEXT_ACTIONS'
  | 'PROJECTS'
  | 'WAITING_FOR'
  | 'SOMEDAY_MAYBE'
  | 'REFERENCE'
  | 'CALENDAR'
  | 'DONE';

export type ContextTheme =
  | 'MINIMALIST'
  | 'DESIGN'
  | 'FORMAL'
  | 'NATURE'
  | 'DARK'
  | 'DRAGONS'
  | 'ICE_DRAGONS';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
}

export interface RegisterResponse {
  id: string;
  email: string;
  createdAt: string;
}

export interface ErrorResponse {
  status: number;
  error: string;
  message: string;
  details?: Record<string, string>;
  timestamp: string;
}

export interface ContextResponse {
  id: string;
  name: string;
  theme: ContextTheme;
  icon: string;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface TaskResponse {
  id: string;
  contextId: string;
  parentTaskId: string | null;
  gtdList: GtdList;
  categoryId: string | null;
  title: string;
  notes: string | null;
  dueDate: string | null;
  reminderSettings: unknown;
  recurrenceRule: unknown;
  nestingLevel: number;
  sortOrder: number;
  isCompleted: boolean;
  completedAt: string | null;
  isRecurring: boolean;
  progress: number | null;
  hasIncompleteSubtasks: boolean | null;
  nextInstanceId: string | null;
  subtasks: TaskResponse[];
  subtaskCount: number | null;
  completedSubtaskCount: number | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface CategoryResponse {
  id: string;
  contextId: string;
  name: string;
  icon: string | null;
  color: string | null;
  sortOrder: number;
  taskCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ReminderResponse {
  id: string;
  taskId: string;
  remindAt: string;
  offsetType: string | null;
  offsetValue: number | null;
  isSent: boolean;
  createdAt: string;
}

export interface TaskCountsResponse {
  byGtdList: Record<string, number>;
  byCategory: Record<string, number>;
  total: number;
}
