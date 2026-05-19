import { useState } from 'react';
import { X, ChevronUp, ChevronDown, Pencil, Trash2, Tag } from 'lucide-react';
import apiClient from '../api/client';
import type { CategoryResponse } from '../types';
import { createElement } from 'react';
import { PRESET_CATEGORY_ICONS, getCategoryIcon } from '../utils/icons';

const PRESET_COLORS = [
  '#EF4444', '#F97316', '#F59E0B', '#22C55E',
  '#3B82F6', '#8B5CF6', '#EC4899', '#6B7280',
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
        <div className="flex items-center justify-between border-b border-stone-200 px-5 py-3.5">
          <h2 className="text-sm font-semibold text-stone-900">
            {mode === 'create' ? 'New Category' : mode === 'edit' ? 'Edit Category' : 'Manage Categories'}
          </h2>
          <button
            type="button"
            onClick={mode === 'list' ? onClose : resetToList}
            className="rounded-md p-1 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="max-h-[60vh] overflow-y-auto px-5 py-4">
          {mode === 'list' && (
            <>
              {categories.length === 0 && (
                <div className="py-6 text-center">
                  <Tag className="mx-auto h-8 w-8 text-stone-300" />
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

              <div className="mb-4">
                <label className="mb-1.5 block text-[11px] font-medium uppercase tracking-wide text-stone-400">
                  Icon
                </label>
                <div className="flex flex-wrap gap-1.5">
                  {PRESET_CATEGORY_ICONS.map(({ key, Icon, label }) => (
                    <button
                      key={key}
                      type="button"
                      onClick={() => setIcon(icon === key ? '' : key)}
                      title={label}
                      className={`flex h-8 w-8 items-center justify-center rounded-md border transition ${
                        icon === key
                          ? 'border-stone-500 bg-stone-100'
                          : 'border-stone-200 hover:border-stone-300 hover:bg-stone-50'
                      }`}
                    >
                      <Icon className="h-4 w-4" />
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
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-stone-50">
          {category.color ? (
            <span
              className="h-4 w-4 rounded-full"
              style={{ backgroundColor: category.color }}
            />
          ) : (
            createElement(getCategoryIcon(category.icon), { className: 'h-4 w-4 text-stone-500' })
          )}
        </div>

        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-medium text-stone-800">{category.name}</p>
          <p className="text-[10px] text-stone-400">
            {category.taskCount} task{category.taskCount !== 1 ? 's' : ''}
          </p>
        </div>

        <div className="flex shrink-0 flex-col gap-0.5 opacity-0 transition group-hover:opacity-100">
          <button
            type="button"
            onClick={onMoveUp}
            disabled={index === 0 || reordering}
            className="rounded p-0.5 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600 disabled:opacity-30"
            title="Move up"
          >
            <ChevronUp className="h-3 w-3" />
          </button>
          <button
            type="button"
            onClick={onMoveDown}
            disabled={index === total - 1 || reordering}
            className="rounded p-0.5 text-stone-400 transition hover:bg-stone-100 hover:text-stone-600 disabled:opacity-30"
            title="Move down"
          >
            <ChevronDown className="h-3 w-3" />
          </button>
        </div>

        <button
          type="button"
          onClick={onEdit}
          className="shrink-0 rounded-md p-1.5 text-stone-400 opacity-0 transition hover:bg-stone-100 hover:text-stone-600 group-hover:opacity-100"
          title="Edit"
        >
          <Pencil className="h-3.5 w-3.5" />
        </button>

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
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    </li>
  );
}
