import { useState } from 'react';
import apiClient from '../api/client';
import type { CategoryResponse } from '../types';

const PRESET_COLORS = [
  '#EF4444', '#F97316', '#F59E0B', '#22C55E',
  '#3B82F6', '#8B5CF6', '#EC4899', '#6B7280',
];

const PRESET_ICONS = [
  '📚', '💼', '🏠', '🎯', '🏃', '🎨',
  '🎵', '💰', '🛒', '✈️', '🔧', '🎓',
];

interface CategoryManagerProps {
  contextId: string;
  categories: CategoryResponse[];
  onCategoriesChanged: () => void;
  onClose: () => void;
}

export default function CategoryManager({
  contextId,
  categories,
  onCategoriesChanged,
  onClose,
}: CategoryManagerProps) {
  const [mode, setMode] = useState<'list' | 'create' | 'edit'>('list');
  const [editingCategory, setEditingCategory] = useState<CategoryResponse | null>(null);
  const [name, setName] = useState('');
  const [icon, setIcon] = useState('');
  const [color, setColor] = useState('#3B82F6');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [reordering, setReordering] = useState(false);

  function openCreate() {
    setName('');
    setIcon('');
    setColor('#3B82F6');
    setError('');
    setMode('create');
  }

  function openEdit(cat: CategoryResponse) {
    setEditingCategory(cat);
    setName(cat.name);
    setIcon(cat.icon ?? '');
    setColor(cat.color ?? '#3B82F6');
    setError('');
    setMode('edit');
  }

  function resetToList() {
    setMode('list');
    setEditingCategory(null);
    setError('');
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) return;

    setSaving(true);
    setError('');
    try {
      await apiClient.post(`/contexts/${contextId}/categories`, {
        name: trimmed,
        icon: icon || null,
        color: color || null,
      });
      onCategoriesChanged();
      resetToList();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Failed to create category');
    } finally {
      setSaving(false);
    }
  }

  async function handleUpdate(e: React.FormEvent) {
    e.preventDefault();
    if (!editingCategory) return;
    const trimmed = name.trim();
    if (!trimmed) return;

    setSaving(true);
    setError('');
    try {
      await apiClient.put(`/categories/${editingCategory.id}`, {
        name: trimmed,
        icon: icon || null,
        color: color || null,
      });
      onCategoriesChanged();
      resetToList();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setError(msg ?? 'Failed to update category');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(cat: CategoryResponse) {
    try {
      await apiClient.delete(`/categories/${cat.id}`);
      onCategoriesChanged();
    } catch { /* silently fail */ }
  }

  async function handleMoveUp(cat: CategoryResponse, index: number) {
    if (index === 0) return;
    setReordering(true);
    try {
      const prevCat = categories[index - 1];
      await Promise.all([
        apiClient.put(`/categories/${cat.id}`, { sortOrder: prevCat.sortOrder }),
        apiClient.put(`/categories/${prevCat.id}`, { sortOrder: cat.sortOrder }),
      ]);
      onCategoriesChanged();
    } catch { /* silently fail */ }
    finally { setReordering(false); }
  }

  async function handleMoveDown(cat: CategoryResponse, index: number) {
    if (index === categories.length - 1) return;
    setReordering(true);
    try {
      const nextCat = categories[index + 1];
      await Promise.all([
        apiClient.put(`/categories/${cat.id}`, { sortOrder: nextCat.sortOrder }),
        apiClient.put(`/categories/${nextCat.id}`, { sortOrder: cat.sortOrder }),
      ]);
      onCategoriesChanged();
    } catch { /* silently fail */ }
    finally { setReordering(false); }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-xl border border-stone-200 bg-white shadow-xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-stone-200 px-5 py-3.5">
          <h2 className="text-sm font-semibold text-stone-900">
            {mode === 'create' ? 'New Category' : mode === 'edit' ? 'Edit Category' : 'Manage Categories'}
          </h2>
          <button
            type="button"
            onClick={mode === 'list' ? onClose : resetToList}
            className="rounded-md p-1 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Body */}
        <div className="max-h-[60vh] overflow-y-auto px-5 py-4">
          {mode === 'list' && (
            <>
              {categories.length === 0 && (
                <div className="py-6 text-center">
                  <p className="text-3xl opacity-30">🏷️</p>
                  <p className="mt-2 text-sm text-stone-400">No categories yet</p>
                  <p className="mt-1 text-xs text-stone-300">Create one to organize your tasks</p>
                </div>
              )}

              <ul className="space-y-1">
                {categories.map((cat, i) => (
                  <CategoryListItem
                    key={cat.id}
                    category={cat}
                    index={i}
                    total={categories.length}
                    reordering={reordering}
                    onEdit={() => openEdit(cat)}
                    onDelete={() => handleDelete(cat)}
                    onMoveUp={() => handleMoveUp(cat, i)}
                    onMoveDown={() => handleMoveDown(cat, i)}
                  />
                ))}
              </ul>

              <button
                type="button"
                onClick={openCreate}
                className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-lg border border-dashed border-stone-300 py-2.5 text-sm font-medium text-stone-500 transition hover:border-stone-400 hover:bg-stone-50 hover:text-stone-700"
              >
                <span>+</span> New Category
              </button>
            </>
          )}

          {(mode === 'create' || mode === 'edit') && (
            <form onSubmit={mode === 'create' ? handleCreate : handleUpdate}>
              {/* Name */}
              <div className="mb-4">
                <label className="mb-1 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
                  Name *
                </label>
                <input
                  autoFocus
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Category name"
                  maxLength={100}
                  className="w-full rounded-md border border-stone-200 px-3 py-2 text-sm text-stone-800 outline-none placeholder:text-stone-400 focus:border-stone-400 focus:ring-1 focus:ring-stone-400/30"
                />
              </div>

              {/* Icon */}
              <div className="mb-4">
                <label className="mb-1.5 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
                  Icon
                </label>
                <div className="flex flex-wrap gap-1.5">
                  {PRESET_ICONS.map((emoji) => (
                    <button
                      key={emoji}
                      type="button"
                      onClick={() => setIcon(icon === emoji ? '' : emoji)}
                      className={`flex h-8 w-8 items-center justify-center rounded-md border text-base transition ${
                        icon === emoji
                          ? 'border-stone-500 bg-stone-100'
                          : 'border-stone-200 hover:border-stone-300 hover:bg-stone-50'
                      }`}
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
                {icon && (
                  <button
                    type="button"
                    onClick={() => setIcon('')}
                    className="mt-1 text-[10px] text-stone-400 hover:text-stone-600"
                  >
                    Clear icon
                  </button>
                )}
              </div>

              {/* Color */}
              <div className="mb-4">
                <label className="mb-1.5 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
                  Color
                </label>
                <div className="flex flex-wrap gap-1.5">
                  {PRESET_COLORS.map((c) => (
                    <button
                      key={c}
                      type="button"
                      onClick={() => setColor(c)}
                      className={`h-7 w-7 rounded-full border-2 transition ${
                        color === c ? 'border-stone-700 scale-110' : 'border-transparent hover:scale-105'
                      }`}
                      style={{ backgroundColor: c }}
                    />
                  ))}
                </div>
                <div className="mt-2 flex items-center gap-2">
                  <span
                    className="h-5 w-5 rounded-full border border-stone-200"
                    style={{ backgroundColor: color }}
                  />
                  <input
                    type="text"
                    value={color}
                    onChange={(e) => setColor(e.target.value.toUpperCase())}
                    placeholder="#RRGGBB"
                    maxLength={7}
                    className="w-24 rounded-md border border-stone-200 px-2 py-1 text-xs text-stone-700 outline-none focus:border-stone-400"
                  />
                </div>
              </div>

              {error && (
                <p className="mb-3 rounded-md bg-red-50 px-3 py-2 text-xs text-red-600">{error}</p>
              )}

              {/* Actions */}
              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={saving || !name.trim()}
                  className="flex-1 rounded-md bg-stone-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-stone-800 disabled:opacity-40"
                >
                  {saving ? 'Saving...' : mode === 'create' ? 'Create' : 'Save Changes'}
                </button>
                <button
                  type="button"
                  onClick={resetToList}
                  className="rounded-md border border-stone-200 px-4 py-2 text-sm font-medium text-stone-600 transition hover:bg-stone-50"
                >
                  Cancel
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

function CategoryListItem({
  category,
  index,
  total,
  reordering,
  onEdit,
  onDelete,
  onMoveUp,
  onMoveDown,
}: {
  category: CategoryResponse;
  index: number;
  total: number;
  reordering: boolean;
  onEdit: () => void;
  onDelete: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
}) {
  const [confirmDelete, setConfirmDelete] = useState(false);

  return (
    <li className="group rounded-lg border border-stone-100 px-3 py-2.5 transition hover:border-stone-200">
      <div className="flex items-center gap-3">
        {/* Color dot / icon */}
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-stone-50">
          {category.icon ? (
            <span className="text-base">{category.icon}</span>
          ) : category.color ? (
            <span
              className="h-4 w-4 rounded-full"
              style={{ backgroundColor: category.color }}
            />
          ) : (
            <span className="text-base">🏷️</span>
          )}
        </div>

        {/* Name and count */}
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-stone-800">{category.name}</p>
          <p className="text-[10px] text-stone-400">
            {category.taskCount} task{category.taskCount !== 1 ? 's' : ''}
          </p>
        </div>

        {/* Reorder arrows */}
        <div className="flex shrink-0 flex-col gap-0.5 opacity-0 transition group-hover:opacity-100">
          <button
            type="button"
            onClick={onMoveUp}
            disabled={index === 0 || reordering}
            className="rounded p-0.5 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600 disabled:opacity-30"
            title="Move up"
          >
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 15l7-7 7 7" />
            </svg>
          </button>
          <button
            type="button"
            onClick={onMoveDown}
            disabled={index === total - 1 || reordering}
            className="rounded p-0.5 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600 disabled:opacity-30"
            title="Move down"
          >
            <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
        </div>

        {/* Edit */}
        <button
          type="button"
          onClick={onEdit}
          className="shrink-0 rounded-md p-1.5 text-stone-400 opacity-0 transition hover:bg-stone-100 hover:text-stone-600 group-hover:opacity-100"
          title="Edit"
        >
          <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
          </svg>
        </button>

        {/* Delete */}
        {confirmDelete ? (
          <div className="flex shrink-0 items-center gap-1">
            <button
              type="button"
              onClick={() => { onDelete(); setConfirmDelete(false); }}
              className="rounded-md bg-red-600 px-2 py-1 text-[10px] font-medium text-white transition hover:bg-red-700"
            >
              Delete
            </button>
            <button
              type="button"
              onClick={() => setConfirmDelete(false)}
              className="rounded-md px-1.5 py-1 text-[10px] text-stone-500 transition hover:bg-stone-100"
            >
              Cancel
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => setConfirmDelete(true)}
            className="shrink-0 rounded-md p-1.5 text-stone-400 opacity-0 transition hover:bg-red-50 hover:text-red-500 group-hover:opacity-100"
            title="Delete"
          >
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        )}
      </div>
    </li>
  );
}
