import {
  Inbox, Zap, FolderOpen, Clock, Lightbulb, BookOpen, Calendar,
  CheckCircle, Tag, Briefcase, Home, Target, Activity, Palette,
  Music, Wallet, ShoppingCart, Plane, Wrench, GraduationCap,
  Dumbbell, Heart, Star,
} from 'lucide-react';
import type { ComponentType } from 'react';
import type { GtdList } from '../types';

export const GTD_ICONS: Record<GtdList, ComponentType<{ className?: string }>> = {
  INBOX: Inbox,
  NEXT_ACTIONS: Zap,
  PROJECTS: FolderOpen,
  WAITING_FOR: Clock,
  SOMEDAY_MAYBE: Lightbulb,
  REFERENCE: BookOpen,
  CALENDAR: Calendar,
  DONE: CheckCircle,
};

const CATEGORY_ICON_MAP: Record<string, ComponentType<{ className?: string }>> = {
  'book-open': BookOpen,
  'briefcase': Briefcase,
  'home': Home,
  'target': Target,
  'activity': Activity,
  'palette': Palette,
  'music': Music,
  'wallet': Wallet,
  'shopping-cart': ShoppingCart,
  'plane': Plane,
  'wrench': Wrench,
  'graduation-cap': GraduationCap,
};

const CONTEXT_ICON_MAP: Record<string, ComponentType<{ className?: string }>> = {
  'briefcase': Briefcase,
  'home': Home,
  'book-open': BookOpen,
  'target': Target,
  'dumbbell': Dumbbell,
  'palette': Palette,
  'music': Music,
  'plane': Plane,
  'lightbulb': Lightbulb,
  'shopping-cart': ShoppingCart,
  'heart': Heart,
  'star': Star,
};

export function getCategoryIcon(iconKey: string | null | undefined): ComponentType<{ className?: string }> {
  if (!iconKey) return Tag;
  return CATEGORY_ICON_MAP[iconKey] ?? Tag;
}

export function getContextIcon(iconKey: string | null | undefined): ComponentType<{ className?: string }> {
  if (!iconKey) return Briefcase;
  return CONTEXT_ICON_MAP[iconKey] ?? Briefcase;
}

export const PRESET_CATEGORY_ICONS: { key: string; Icon: ComponentType<{ className?: string }>; label: string }[] = [
  { key: 'book-open', Icon: BookOpen, label: 'Books' },
  { key: 'briefcase', Icon: Briefcase, label: 'Work' },
  { key: 'home', Icon: Home, label: 'Home' },
  { key: 'target', Icon: Target, label: 'Goals' },
  { key: 'activity', Icon: Activity, label: 'Fitness' },
  { key: 'palette', Icon: Palette, label: 'Art' },
  { key: 'music', Icon: Music, label: 'Music' },
  { key: 'wallet', Icon: Wallet, label: 'Finance' },
  { key: 'shopping-cart', Icon: ShoppingCart, label: 'Shopping' },
  { key: 'plane', Icon: Plane, label: 'Travel' },
  { key: 'wrench', Icon: Wrench, label: 'Tools' },
  { key: 'graduation-cap', Icon: GraduationCap, label: 'Study' },
];

export const PRESET_CONTEXT_ICONS: { key: string; Icon: ComponentType<{ className?: string }>; label: string }[] = [
  { key: 'briefcase', Icon: Briefcase, label: 'Work' },
  { key: 'home', Icon: Home, label: 'Home' },
  { key: 'book-open', Icon: BookOpen, label: 'Books' },
  { key: 'target', Icon: Target, label: 'Goals' },
  { key: 'dumbbell', Icon: Dumbbell, label: 'Fitness' },
  { key: 'palette', Icon: Palette, label: 'Art' },
  { key: 'music', Icon: Music, label: 'Music' },
  { key: 'plane', Icon: Plane, label: 'Travel' },
  { key: 'lightbulb', Icon: Lightbulb, label: 'Ideas' },
  { key: 'shopping-cart', Icon: ShoppingCart, label: 'Shopping' },
  { key: 'heart', Icon: Heart, label: 'Health' },
  { key: 'star', Icon: Star, label: 'Favorites' },
];
