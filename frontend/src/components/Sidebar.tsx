import { useDroppable } from '@dnd-kit/core';
import {
  Inbox, Zap, FolderOpen, Clock, Lightbulb, BookOpen, Calendar,
  CheckCircle, Settings, Leaf,
} from 'lucide-react';
import { createElement } from 'react';
import type { ComponentType } from 'react';
import type { CategoryResponse, GtdList, TaskCountsResponse } from '../types';
import type { ThemeColors } from '../utils/themes';
import { getCategoryIcon } from '../utils/icons';

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

  const activeExtraClass = isActive && theme.sidebarActiveStyle ? theme.sidebarActiveStyle : '';
  const badgeBlobClass = theme.blobBadge ? 'theme-nature-blob' : 'rounded-full';

  return (
    <button
      ref={setNodeRef}
      type="button"
      onClick={onClick}
      className={`group flex w-full items-center gap-2 ${theme.borderRadius} px-2 py-1.5 text-left text-sm transition ${theme.transitionSpeed} ${activeExtraClass} ${
        isOver
          ? `${theme.dropTargetBg} ${theme.dropTargetRing}`
          : isActive
            ? `${theme.sidebarItemActive} ${theme.sidebarItemActiveText}`
            : isDimmed
              ? `${theme.sidebarLabel} ${theme.sidebarItemHover}`
              : `${theme.sidebarItemText} ${theme.sidebarItemHover}`
      }`}
    >
      {isActive && theme.leafIcon && <Leaf className="h-3 w-3 shrink-0 text-emerald-500" />}
      <Icon className="h-4 w-4 shrink-0" />
      <span className="flex-1 truncate">{label}</span>
      {count > 0 && (
        <span className={`min-w-[20px] ${badgeBlobClass} px-1.5 text-center text-xs ${theme.badgeFont} ${
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
    <aside className={`flex h-full w-56 shrink-0 flex-col border-r ${theme.sidebarBorder} ${theme.sidebarBg} ${theme.sidebarTopBorder}`}>
      {theme.breathingBg && (
        <div className="theme-nature-breathing pointer-events-none absolute inset-0 bg-gradient-to-b from-emerald-200 to-transparent" />
      )}
      <nav className="relative flex-1 overflow-y-auto px-2 py-3">
        <div className={`mb-1 px-2 text-[10px] font-semibold uppercase tracking-wider ${theme.sidebarLabel} ${theme.labelStyle} ${theme.sidebarSectionUnderline}`}>
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

        {theme.sidebarDividerStyle === 'theme-design-gradient-divider' ? (
          <div className="my-2 theme-design-gradient-divider" />
        ) : theme.sidebarDividerStyle === 'theme-dragons-claw-marks' ? (
          <div className="my-2 theme-dragons-claw-marks" />
        ) : theme.sidebarDividerStyle === 'gap' ? (
          <div className="my-3" />
        ) : (
          <div className={`my-2 border-t ${theme.sidebarDivider}`} />
        )}

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

        <div className={`mb-1 mt-4 flex items-center justify-between px-2 ${theme.sidebarSectionUnderline}`}>
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
            className={`mx-2 ${theme.borderRadius} ${theme.decorativeBorder} border-dashed ${theme.inputBorder} px-2 py-2 text-[11px] ${theme.sidebarLabel} transition ${theme.transitionSpeed} hover:opacity-80`}
          >
            + Add a category
          </button>
        )}

        {categories.map((cat) => {
          const isCatActive = activeSection === `cat:${cat.id}`;
          const catActiveExtra = isCatActive && theme.sidebarActiveStyle ? theme.sidebarActiveStyle : '';
          const catBadgeBlobClass = theme.blobBadge ? 'theme-nature-blob' : 'rounded-full';
          return (
            <button
              key={cat.id}
              type="button"
              onClick={() => onSectionChange(`cat:${cat.id}`)}
              className={`group flex w-full items-center gap-2 ${theme.borderRadius} px-2 py-1.5 text-left text-sm transition ${theme.transitionSpeed} ${catActiveExtra} ${
                isCatActive
                  ? `${theme.sidebarItemActive} ${theme.sidebarItemActiveText}`
                  : `${theme.sidebarItemText} ${theme.sidebarItemHover}`
              }`}
            >
              {createElement(getCategoryIcon(cat.icon), {
                className: 'h-4 w-4 shrink-0',
                ...(cat.color ? { style: { color: cat.color } } : {}),
              })}
              <span className="flex-1 truncate">{cat.name}</span>
              {catCount(cat.id) > 0 && (
                <span className={`min-w-[20px] ${catBadgeBlobClass} px-1.5 text-center text-xs ${theme.badgeFont} ${
                  isCatActive ? theme.sidebarBadgeActive : theme.sidebarBadge
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
