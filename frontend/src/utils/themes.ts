import type { ContextTheme } from '../types';

export interface ThemeColors {
  bg: string;
  headerBg: string;
  headerBorder: string;
  headerText: string;
  headerSubtext: string;
  sidebarBg: string;
  sidebarBorder: string;
  sidebarLabel: string;
  sidebarItemText: string;
  sidebarItemHover: string;
  sidebarItemActive: string;
  sidebarItemActiveText: string;
  sidebarBadge: string;
  sidebarBadgeActive: string;
  sidebarDivider: string;
  mainBg: string;
  listHeaderBorder: string;
  listHeaderText: string;
  listCountBadge: string;
  taskText: string;
  taskCompletedText: string;
  taskSubtext: string;
  taskHover: string;
  taskSelected: string;
  taskDivider: string;
  checkbox: string;
  checkboxChecked: string;
  progressBg: string;
  progressFill: string;
  inputBorder: string;
  inputFocus: string;
  inputBg: string;
  inputText: string;
  inputPlaceholder: string;
  btnPrimary: string;
  btnPrimaryHover: string;
  btnPrimaryText: string;
  labelText: string;
  panelBg: string;
  panelBorder: string;
  emptyIcon: string;
  emptyText: string;
  dropTargetBg: string;
  dropTargetRing: string;
  borderRadius: string;
  borderRadiusLg: string;
  shadow: string;
  shadowLg: string;
  fontFamily: string;
  headerFont: string;
  spacing: string;
  labelStyle: string;
  decorativeBorder: string;
  cardStyle: string;
}

const MINIMALIST: ThemeColors = {
  bg: 'bg-stone-50',
  headerBg: 'bg-white',
  headerBorder: 'border-stone-200',
  headerText: 'text-stone-900',
  headerSubtext: 'text-stone-400',
  sidebarBg: 'bg-white',
  sidebarBorder: 'border-stone-200',
  sidebarLabel: 'text-stone-400',
  sidebarItemText: 'text-stone-600',
  sidebarItemHover: 'hover:bg-stone-50 hover:text-stone-900',
  sidebarItemActive: 'bg-stone-100',
  sidebarItemActiveText: 'font-medium text-stone-900',
  sidebarBadge: 'bg-stone-100 text-stone-500',
  sidebarBadgeActive: 'bg-stone-200 text-stone-700',
  sidebarDivider: 'border-stone-100',
  mainBg: 'bg-white',
  listHeaderBorder: 'border-stone-200',
  listHeaderText: 'text-stone-900',
  listCountBadge: 'bg-stone-100 text-stone-500',
  taskText: 'text-stone-800',
  taskCompletedText: 'text-stone-400',
  taskSubtext: 'text-stone-400',
  taskHover: 'hover:bg-stone-50/50',
  taskSelected: 'bg-stone-50',
  taskDivider: 'divide-stone-100',
  checkbox: 'border-stone-300 hover:border-stone-500',
  checkboxChecked: 'border-stone-300 bg-stone-200 text-stone-500',
  progressBg: 'bg-stone-200',
  progressFill: 'bg-stone-500',
  inputBorder: 'border-stone-200',
  inputFocus: 'focus:border-stone-400 focus:ring-stone-400/30',
  inputBg: 'bg-white',
  inputText: 'text-stone-800',
  inputPlaceholder: 'placeholder:text-stone-400',
  btnPrimary: 'bg-stone-900',
  btnPrimaryHover: 'hover:bg-stone-800',
  btnPrimaryText: 'text-white',
  labelText: 'text-stone-400',
  panelBg: 'bg-white',
  panelBorder: 'border-stone-200',
  emptyIcon: 'opacity-30',
  emptyText: 'text-stone-400',
  dropTargetBg: 'bg-blue-50',
  dropTargetRing: 'ring-1 ring-blue-300',
  borderRadius: 'rounded-none',
  borderRadiusLg: 'rounded-sm',
  shadow: 'shadow-none',
  shadowLg: 'shadow-none',
  fontFamily: 'font-sans',
  headerFont: 'font-sans',
  spacing: 'p-3',
  labelStyle: '',
  decorativeBorder: 'border',
  cardStyle: 'border',
};

const DESIGN: ThemeColors = {
  bg: 'bg-violet-50/40',
  headerBg: 'bg-white',
  headerBorder: 'border-violet-100',
  headerText: 'text-violet-900',
  headerSubtext: 'text-violet-400',
  sidebarBg: 'bg-violet-50/30',
  sidebarBorder: 'border-violet-100',
  sidebarLabel: 'text-violet-400',
  sidebarItemText: 'text-violet-600',
  sidebarItemHover: 'hover:bg-violet-100/50 hover:text-violet-900',
  sidebarItemActive: 'bg-violet-100',
  sidebarItemActiveText: 'font-medium text-violet-900',
  sidebarBadge: 'bg-violet-100 text-violet-500',
  sidebarBadgeActive: 'bg-violet-200 text-violet-700',
  sidebarDivider: 'border-violet-100',
  mainBg: 'bg-white',
  listHeaderBorder: 'border-violet-100',
  listHeaderText: 'text-violet-900',
  listCountBadge: 'bg-violet-100 text-violet-500',
  taskText: 'text-violet-800',
  taskCompletedText: 'text-violet-300',
  taskSubtext: 'text-violet-400',
  taskHover: 'hover:bg-violet-50/50',
  taskSelected: 'bg-violet-50',
  taskDivider: 'divide-violet-100',
  checkbox: 'border-violet-300 hover:border-violet-500',
  checkboxChecked: 'border-violet-300 bg-violet-200 text-violet-500',
  progressBg: 'bg-violet-200',
  progressFill: 'bg-violet-500',
  inputBorder: 'border-violet-200',
  inputFocus: 'focus:border-violet-400 focus:ring-violet-400/30',
  inputBg: 'bg-white',
  inputText: 'text-violet-800',
  inputPlaceholder: 'placeholder:text-violet-400',
  btnPrimary: 'bg-violet-600',
  btnPrimaryHover: 'hover:bg-violet-700',
  btnPrimaryText: 'text-white',
  labelText: 'text-violet-400',
  panelBg: 'bg-white',
  panelBorder: 'border-violet-100',
  emptyIcon: 'opacity-30',
  emptyText: 'text-violet-400',
  dropTargetBg: 'bg-violet-50',
  dropTargetRing: 'ring-1 ring-violet-300',
  borderRadius: 'rounded-2xl',
  borderRadiusLg: 'rounded-3xl',
  shadow: 'shadow-lg',
  shadowLg: 'shadow-xl',
  fontFamily: 'font-sans',
  headerFont: 'font-sans',
  spacing: 'p-5',
  labelStyle: 'tracking-wide',
  decorativeBorder: 'border-2',
  cardStyle: 'border-2 bg-gradient-to-br from-white to-violet-50/50',
};

const FORMAL: ThemeColors = {
  bg: 'bg-slate-50',
  headerBg: 'bg-white',
  headerBorder: 'border-slate-200',
  headerText: 'text-slate-900',
  headerSubtext: 'text-slate-400',
  sidebarBg: 'bg-slate-50/60',
  sidebarBorder: 'border-slate-200',
  sidebarLabel: 'text-slate-400',
  sidebarItemText: 'text-slate-600',
  sidebarItemHover: 'hover:bg-slate-100/60 hover:text-slate-900',
  sidebarItemActive: 'bg-slate-100',
  sidebarItemActiveText: 'font-medium text-slate-900',
  sidebarBadge: 'bg-slate-100 text-slate-500',
  sidebarBadgeActive: 'bg-slate-200 text-slate-700',
  sidebarDivider: 'border-slate-100',
  mainBg: 'bg-white',
  listHeaderBorder: 'border-slate-200',
  listHeaderText: 'text-slate-900',
  listCountBadge: 'bg-slate-100 text-slate-500',
  taskText: 'text-slate-800',
  taskCompletedText: 'text-slate-400',
  taskSubtext: 'text-slate-400',
  taskHover: 'hover:bg-slate-50/50',
  taskSelected: 'bg-slate-50',
  taskDivider: 'divide-slate-100',
  checkbox: 'border-slate-300 hover:border-slate-500',
  checkboxChecked: 'border-slate-300 bg-slate-200 text-slate-500',
  progressBg: 'bg-slate-200',
  progressFill: 'bg-slate-500',
  inputBorder: 'border-slate-200',
  inputFocus: 'focus:border-slate-400 focus:ring-slate-400/30',
  inputBg: 'bg-white',
  inputText: 'text-slate-800',
  inputPlaceholder: 'placeholder:text-slate-400',
  btnPrimary: 'bg-slate-800',
  btnPrimaryHover: 'hover:bg-slate-900',
  btnPrimaryText: 'text-white',
  labelText: 'text-slate-400',
  panelBg: 'bg-white',
  panelBorder: 'border-slate-200',
  emptyIcon: 'opacity-30',
  emptyText: 'text-slate-400',
  dropTargetBg: 'bg-blue-50',
  dropTargetRing: 'ring-1 ring-blue-300',
  borderRadius: 'rounded-md',
  borderRadiusLg: 'rounded-lg',
  shadow: 'shadow-sm',
  shadowLg: 'shadow-md',
  fontFamily: 'font-sans',
  headerFont: 'font-serif',
  spacing: 'p-4',
  labelStyle: 'uppercase tracking-wider',
  decorativeBorder: 'border-double border-2',
  cardStyle: 'border-double border-2',
};

const NATURE: ThemeColors = {
  bg: 'bg-emerald-50/40',
  headerBg: 'bg-white',
  headerBorder: 'border-emerald-100',
  headerText: 'text-emerald-900',
  headerSubtext: 'text-emerald-400',
  sidebarBg: 'bg-emerald-50/30',
  sidebarBorder: 'border-emerald-100',
  sidebarLabel: 'text-emerald-500',
  sidebarItemText: 'text-emerald-700',
  sidebarItemHover: 'hover:bg-emerald-100/40 hover:text-emerald-900',
  sidebarItemActive: 'bg-emerald-100',
  sidebarItemActiveText: 'font-medium text-emerald-900',
  sidebarBadge: 'bg-emerald-100 text-emerald-600',
  sidebarBadgeActive: 'bg-emerald-200 text-emerald-700',
  sidebarDivider: 'border-emerald-100',
  mainBg: 'bg-white',
  listHeaderBorder: 'border-emerald-100',
  listHeaderText: 'text-emerald-900',
  listCountBadge: 'bg-emerald-100 text-emerald-600',
  taskText: 'text-emerald-800',
  taskCompletedText: 'text-emerald-300',
  taskSubtext: 'text-emerald-400',
  taskHover: 'hover:bg-emerald-50/50',
  taskSelected: 'bg-emerald-50',
  taskDivider: 'divide-emerald-100',
  checkbox: 'border-emerald-300 hover:border-emerald-500',
  checkboxChecked: 'border-emerald-300 bg-emerald-200 text-emerald-500',
  progressBg: 'bg-emerald-200',
  progressFill: 'bg-emerald-500',
  inputBorder: 'border-emerald-200',
  inputFocus: 'focus:border-emerald-400 focus:ring-emerald-400/30',
  inputBg: 'bg-white',
  inputText: 'text-emerald-800',
  inputPlaceholder: 'placeholder:text-emerald-400',
  btnPrimary: 'bg-emerald-600',
  btnPrimaryHover: 'hover:bg-emerald-700',
  btnPrimaryText: 'text-white',
  labelText: 'text-emerald-500',
  panelBg: 'bg-white',
  panelBorder: 'border-emerald-100',
  emptyIcon: 'opacity-30',
  emptyText: 'text-emerald-400',
  dropTargetBg: 'bg-emerald-50',
  dropTargetRing: 'ring-1 ring-emerald-300',
  borderRadius: 'rounded-xl',
  borderRadiusLg: 'rounded-2xl',
  shadow: 'shadow-md shadow-emerald-100/50',
  shadowLg: 'shadow-lg shadow-emerald-100/50',
  fontFamily: 'font-sans',
  headerFont: 'font-sans',
  spacing: 'p-5',
  labelStyle: '',
  decorativeBorder: 'border',
  cardStyle: 'border',
};

const DARK: ThemeColors = {
  bg: 'bg-zinc-900',
  headerBg: 'bg-zinc-800',
  headerBorder: 'border-zinc-700',
  headerText: 'text-zinc-100',
  headerSubtext: 'text-zinc-400',
  sidebarBg: 'bg-zinc-800',
  sidebarBorder: 'border-zinc-700',
  sidebarLabel: 'text-zinc-500',
  sidebarItemText: 'text-zinc-400',
  sidebarItemHover: 'hover:bg-zinc-700 hover:text-zinc-200',
  sidebarItemActive: 'bg-zinc-700',
  sidebarItemActiveText: 'font-medium text-zinc-100',
  sidebarBadge: 'bg-zinc-700 text-zinc-400',
  sidebarBadgeActive: 'bg-zinc-600 text-zinc-200',
  sidebarDivider: 'border-zinc-700',
  mainBg: 'bg-zinc-900',
  listHeaderBorder: 'border-zinc-700',
  listHeaderText: 'text-zinc-100',
  listCountBadge: 'bg-zinc-700 text-zinc-300',
  taskText: 'text-zinc-200',
  taskCompletedText: 'text-zinc-500',
  taskSubtext: 'text-zinc-500',
  taskHover: 'hover:bg-zinc-800/60',
  taskSelected: 'bg-zinc-800',
  taskDivider: 'divide-zinc-800',
  checkbox: 'border-zinc-500 hover:border-zinc-300',
  checkboxChecked: 'border-zinc-500 bg-zinc-600 text-zinc-300',
  progressBg: 'bg-zinc-700',
  progressFill: 'bg-zinc-400',
  inputBorder: 'border-zinc-600',
  inputFocus: 'focus:border-zinc-400 focus:ring-zinc-400/30',
  inputBg: 'bg-zinc-800',
  inputText: 'text-zinc-200',
  inputPlaceholder: 'placeholder:text-zinc-500',
  btnPrimary: 'bg-zinc-100',
  btnPrimaryHover: 'hover:bg-white',
  btnPrimaryText: 'text-zinc-900',
  labelText: 'text-zinc-500',
  panelBg: 'bg-zinc-800',
  panelBorder: 'border-zinc-700',
  emptyIcon: 'opacity-20',
  emptyText: 'text-zinc-500',
  dropTargetBg: 'bg-zinc-700',
  dropTargetRing: 'ring-1 ring-zinc-500',
  borderRadius: 'rounded-lg',
  borderRadiusLg: 'rounded-xl',
  shadow: 'shadow-lg shadow-black/30',
  shadowLg: 'shadow-xl shadow-black/40',
  fontFamily: 'font-sans',
  headerFont: 'font-sans',
  spacing: 'p-4',
  labelStyle: '',
  decorativeBorder: 'border',
  cardStyle: 'border backdrop-blur-md bg-zinc-800/80',
};

const THEME_MAP: Record<ContextTheme, ThemeColors> = {
  MINIMALIST,
  DESIGN,
  FORMAL,
  NATURE,
  DARK,
};

export function getTheme(theme: ContextTheme): ThemeColors {
  return THEME_MAP[theme] ?? MINIMALIST;
}

export const THEME_NAMES: Record<ContextTheme, string> = {
  MINIMALIST: 'Minimalist',
  DESIGN: 'Design',
  FORMAL: 'Formal',
  NATURE: 'Nature',
  DARK: 'Dark',
};
