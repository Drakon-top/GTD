import type { GtdList } from '../types';

const GTD_LABELS: Record<GtdList, string> = {
  INBOX: 'Inbox',
  NEXT_ACTIONS: 'Next Actions',
  PROJECTS: 'Projects',
  WAITING_FOR: 'Waiting For',
  SOMEDAY_MAYBE: 'Someday / Maybe',
  REFERENCE: 'Reference',
  CALENDAR: 'Calendar',
  DONE: 'Done',
};

export function gtdListLabel(list: GtdList): string {
  return GTD_LABELS[list] ?? list;
}
