import { useDroppable } from '@dnd-kit/core';
import type { CategoryResponse, GtdList, TaskCountsResponse } from '../types';
import type { ThemeColors } from '../utils/themes';

const GTD_LISTS: { key: GtdList; label: string; icon: string }[] = [
  { key: 'INBOX', label: 'Inbox', icon: '📥' },
  { key: 'NEXT_ACTIONS', label: 'Next Actions', icon: '⚡' },
  { key: 'PROJECTS', label: 'Projects', icon: '📁' },
  { key: 'WAITING_FOR', label: 'Waiting For', icon: '⏳' },
  { key: 'SOMEDAY_MAYBE', label: 'Someday / Maybe', icon: '💭' },
  { key: 'REFERENCE', label: 'Reference', icon: '📎' },
  { key: 'CALENDAR', label: 'Calendar', icon: '📅' },
];

const DONE_LIST: { key: GtdList; label: string; icon: string } = {
  key: 'DONE',
  label: 'Done',
  icon: '✅',
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
  icon,
  count,
  isActive,
  isDimmed,
  onClick,
  theme,
}: {
  gtdKey: GtdList;
  label: string;
  icon: string;
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
      className={`group flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition ${
        isOver
          ? `${theme.dropTargetBg} ${theme.dropTargetRing}`
          : isActive
            ? `${theme.sidebarItemActive} ${theme.sidebarItemActiveText}`
            : isDimmed
              ? `${theme.sidebarLabel} ${theme.sidebarItemHover}`
              : `${theme.sidebarItemText} ${theme.sidebarItemHover}`
      }`}
    >
      <span className="w-5 text-center text-sm">{icon}</span>
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
        <div className={`mb-1 px-2 text-[10px] font-semibold uppercase tracking-wider ${theme.sidebarLabel}`}>
          GTD Lists
        </div>

        {GTD_LISTS.map(({ key, label, icon }) => (
          <DroppableGtdItem
            key={key}
            gtdKey={key}
            label={label}
            icon={icon}
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
          icon={DONE_LIST.icon}
          count={gtdCount('DONE')}
          isActive={activeSection === 'DONE'}
          isDimmed
          onClick={() => onSectionChange(DONE_LIST.key)}
          theme={theme}
        />

        <div className="mb-1 mt-4 flex items-center justify-between px-2">
          <span className={`text-[10px] font-semibold uppercase tracking-wider ${theme.sidebarLabel}`}>
            Categories
          </span>
          <button
            type="button"
            onClick={onManageCategories}
            className={`rounded p-0.5 ${theme.sidebarLabel} transition hover:opacity-80`}
            title="Manage categories"
          >
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          </button>
        </div>

        {categories.length === 0 && (
          <button
            type="button"
            onClick={onManageCategories}
            className={`mx-2 rounded-md border border-dashed ${theme.inputBorder} px-2 py-2 text-[11px] ${theme.sidebarLabel} transition hover:opacity-80`}
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
              className={`group flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition ${
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
                <span className="w-5 text-center text-sm">{cat.icon || '🏷️'}</span>
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
