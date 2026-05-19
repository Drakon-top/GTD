import { useDroppable } from '@dnd-kit/core';
import {
  Inbox, Zap, FolderOpen, Clock, Lightbulb, BookOpen, Calendar,
  CheckCircle, Settings, Tag,
} from 'lucide-react';
import type { ComponentType } from 'react';
import type { CategoryResponse, GtdList, TaskCountsResponse } from '../types';
import type { ThemeColors } from '../utils/themes';

const GTD_LISTS: { key: GtdList; label: string; Icon: ComponentType<{ className?: string }> }[] = [
  { key: 'INBOX', label: 'Inbox', Icon: Inbox },
  { key: 'NEXT_ACTIONS', label: 'Next Actions', Icon: Zap },
  { key: 'PROJECTS', label: 'Projects', Icon: FolderOpen },
  { key: 'WAITING_FOR', label: 'Waiting For', Icon: Clock },
  { key: 'SOMEDAY_MAYBE', label: 'Someday / Maybe', Icon: Lightbulb },
  { key: 'REFERENCE', label: 'Reference', Icon: BookOpen },
  { key: 'CALENDAR', label: 'Calendar', Icon: Calendar },
];

const DONE_LIST = {
  key: 'DONE' as GtdList,
  label: 'Done',
  Icon: CheckCircle,
};

interface SidebarProps {
  counts: TaskCountsResponse | null;
  categories: CategoryResponse[];
  activeSection: string;
  onSectionChange: (section: string) => void;
  onManageCategories: () => void;
  theme: ThemeColors;
}

function DroppableGtdItem({
  gtdKey,
  label,
  Icon,
  count,
  isActive,
  isDimmed,
  onClick,
  theme,
}: {
  gtdKey: GtdList;
  label: string;
  Icon: ComponentType<{ className?: string }>;
  count: number;
  isActive: boolean;
  isDimmed?: boolean;
  onClick: () => void;
  theme: ThemeColors;
}) {
  const { isOver, setNodeRef } = useDroppable({ id: `gtd:${gtdKey}` });

  return (
    <button
      ref={setNodeRef}
      type="button"
      onClick={onClick}
      className={`group flex w-full items-center gap-2 ${theme.borderRadius} px-2 py-1.5 text-left text-sm transition ${
        isOver
          ? `${theme.dropTargetBg} ${theme.dropTargetRing}`
          : isActive
            ? `${theme.sidebarItemActive} ${theme.sidebarItemActiveText}`
            : isDimmed
              ? `${theme.sidebarLabel} ${theme.sidebarItemHover}`
              : `${theme.sidebarItemText} ${theme.sidebarItemHover}`
      }`}
    >
      <Icon className="h-4 w-4 shrink-0" />
      <span className="flex-1 truncate">{label}</span>
      {count > 0 && (
        <span className={`min-w-[20px] rounded-full px-1.5 text-center text-xs ${
          isActive ? theme.sidebarBadgeActive : theme.sidebarBadge
        }`}>
          {count}
        </span>
      )}
    </button>
  );
}

export default function Sidebar({ counts, categories, activeSection, onSectionChange, onManageCategories, theme }: SidebarProps) {
  const gtdCount = (key: GtdList) => counts?.byGtdList[key] ?? 0;
  const catCount = (id: string) => counts?.byCategory[id] ?? 0;

  return (
    <aside className={`flex h-full w-56 shrink-0 flex-col border-r ${theme.sidebarBorder} ${theme.sidebarBg}`}>
      <nav className="flex-1 overflow-y-auto px-2 py-3">
        <div className={`mb-1 px-2 text-[10px] font-semibold uppercase tracking-wider ${theme.sidebarLabel} ${theme.labelStyle}`}>
          GTD Lists
        </div>

        {GTD_LISTS.map(({ key, label, Icon }) => (
          <DroppableGtdItem
            key={key}
            gtdKey={key}
            label={label}
            Icon={Icon}
            count={gtdCount(key)}
            isActive={activeSection === key}
            onClick={() => onSectionChange(key)}
            theme={theme}
          />
        ))}

        <div className={`my-2 border-t ${theme.sidebarDivider}`} />

        <DroppableGtdItem
          gtdKey={DONE_LIST.key}
          label={DONE_LIST.label}
          Icon={DONE_LIST.Icon}
          count={gtdCount('DONE')}
          isActive={activeSection === 'DONE'}
          isDimmed
          onClick={() => onSectionChange(DONE_LIST.key)}
          theme={theme}
        />

        <div className="mb-1 mt-4 flex items-center justify-between px-2">
          <span className={`text-[10px] font-semibold uppercase tracking-wider ${theme.sidebarLabel} ${theme.labelStyle}`}>
            Categories
          </span>
          <button
            type="button"
            onClick={onManageCategories}
            className={`rounded p-0.5 ${theme.sidebarLabel} transition hover:opacity-80`}
            title="Manage categories"
          >
            <Settings className="h-3.5 w-3.5" />
          </button>
        </div>

        {categories.length === 0 && (
          <button
            type="button"
            onClick={onManageCategories}
            className={`mx-2 ${theme.borderRadius} border border-dashed ${theme.inputBorder} px-2 py-2 text-[11px] ${theme.sidebarLabel} transition hover:opacity-80`}
          >
            + Add a category
          </button>
        )}

        {categories.map((cat) => {
          const isActive = activeSection === `cat:${cat.id}`;
          return (
            <button
              key={cat.id}
              type="button"
              onClick={() => onSectionChange(`cat:${cat.id}`)}
              className={`group flex w-full items-center gap-2 ${theme.borderRadius} px-2 py-1.5 text-left text-sm transition ${
                isActive
                  ? `${theme.sidebarItemActive} ${theme.sidebarItemActiveText}`
                  : `${theme.sidebarItemText} ${theme.sidebarItemHover}`
              }`}
            >
              {cat.color ? (
                <span
                  className={`h-3 w-3 rounded-full border ${theme.sidebarDivider}`}
                  style={{ backgroundColor: cat.color }}
                />
              ) : (
                <Tag className="h-4 w-4 shrink-0" />
              )}
              <span className="flex-1 truncate">{cat.name}</span>
              {catCount(cat.id) > 0 && (
                <span className={`min-w-[20px] rounded-full px-1.5 text-center text-xs ${
                  isActive ? theme.sidebarBadgeActive : theme.sidebarBadge
                }`}>
                  {catCount(cat.id)}
                </span>
              )}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
