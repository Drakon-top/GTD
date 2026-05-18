import { useDroppable } from '@dnd-kit/core';
import type { CategoryResponse, GtdList, TaskCountsResponse } from '../types';

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
}

function DroppableGtdItem({
  gtdKey,
  label,
  icon,
  count,
  isActive,
  isDimmed,
  onClick,
}: {
  gtdKey: GtdList;
  label: string;
  icon: string;
  count: number;
  isActive: boolean;
  isDimmed?: boolean;
  onClick: () => void;
}) {
  const { isOver, setNodeRef } = useDroppable({ id: `gtd:${gtdKey}` });

  return (
    <button
      ref={setNodeRef}
      type="button"
      onClick={onClick}
      className={`group flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition ${
        isOver
          ? 'bg-blue-50 ring-1 ring-blue-300'
          : isActive
            ? 'bg-stone-100 font-medium text-stone-900'
            : isDimmed
              ? 'text-stone-400 hover:bg-stone-50 hover:text-stone-600'
              : 'text-stone-600 hover:bg-stone-50 hover:text-stone-900'
      }`}
    >
      <span className="w-5 text-center text-sm">{icon}</span>
      <span className="flex-1 truncate">{label}</span>
      {count > 0 && (
        <span className={`min-w-[20px] rounded-full px-1.5 text-center text-xs ${
          isActive ? 'bg-stone-200 text-stone-700' : 'bg-stone-100 text-stone-500'
        }`}>
          {count}
        </span>
      )}
    </button>
  );
}

export default function Sidebar({ counts, categories, activeSection, onSectionChange }: SidebarProps) {
  const gtdCount = (key: GtdList) => counts?.byGtdList[key] ?? 0;
  const catCount = (id: string) => counts?.byCategory[id] ?? 0;

  return (
    <aside className="flex h-full w-56 shrink-0 flex-col border-r border-stone-200 bg-white">
      <nav className="flex-1 overflow-y-auto px-2 py-3">
        <div className="mb-1 px-2 text-[10px] font-semibold uppercase tracking-wider text-stone-400">
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
          />
        ))}

        <div className="my-2 border-t border-stone-100" />

        <DroppableGtdItem
          gtdKey={DONE_LIST.key}
          label={DONE_LIST.label}
          icon={DONE_LIST.icon}
          count={gtdCount('DONE')}
          isActive={activeSection === 'DONE'}
          isDimmed
          onClick={() => onSectionChange(DONE_LIST.key)}
        />

        {categories.length > 0 && (
          <>
            <div className="mb-1 mt-4 px-2 text-[10px] font-semibold uppercase tracking-wider text-stone-400">
              Categories
            </div>
            {categories.map((cat) => {
              const isActive = activeSection === `cat:${cat.id}`;
              return (
                <button
                  key={cat.id}
                  type="button"
                  onClick={() => onSectionChange(`cat:${cat.id}`)}
                  className={`group flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm transition ${
                    isActive
                      ? 'bg-stone-100 font-medium text-stone-900'
                      : 'text-stone-600 hover:bg-stone-50 hover:text-stone-900'
                  }`}
                >
                  {cat.color ? (
                    <span
                      className="h-3 w-3 rounded-full border border-stone-200"
                      style={{ backgroundColor: cat.color }}
                    />
                  ) : (
                    <span className="w-5 text-center text-sm">{cat.icon || '🏷️'}</span>
                  )}
                  <span className="flex-1 truncate">{cat.name}</span>
                  {catCount(cat.id) > 0 && (
                    <span className={`min-w-[20px] rounded-full px-1.5 text-center text-xs ${
                      isActive ? 'bg-stone-200 text-stone-700' : 'bg-stone-100 text-stone-500'
                    }`}>
                      {catCount(cat.id)}
                    </span>
                  )}
                </button>
              );
            })}
          </>
        )}
      </nav>
    </aside>
  );
}
