import { useState } from 'react';
import { X, ChevronUp, ChevronDown, Pencil, Trash2, Tag } from 'lucide-react';
import apiClient from '../api/client';
import type { CategoryResponse } from '../types';
import { createElement } from 'react';
import { PRESET_CATEGORY_ICONS, getCategoryIcon } from '../utils/icons';
import type { ThemeColors } from '../utils/themes';

const PRESET_COLORS = [
  '#EF4444', '#F97316', '#F59E0B', '#22C55E',
  '#3B82F6', '#8B5CF6', '#EC4899', '#6B7280',
];

interface CategoryManagerProps {
  contextId: string;
  categories: CategoryResponse[];
  onCategoriesChanged: () => void;
  onClose: () => void;
  theme: ThemeColors;
}

export default function CategoryManager({
  contextId,
  categories,
  onCategoriesChanged,
  onClose,
  theme,
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
      <div className={`w-full max-w-md ${theme.borderRadiusLg} ${theme.cardStyle} ${theme.inputBorder} ${theme.panelBg} ${theme.shadowLg}`}>
        <div className={`flex items-center justify-between border-b ${theme.panelBorder} px-5 py-3.5`}>
          <h2 className={`text-sm font-semibold ${theme.headerFont} ${theme.headerText}`}>
            {mode === 'create' ? 'New Category' : mode === 'edit' ? 'Edit Category' : 'Manage Categories'}
          </h2>
          <button
            type="button"
            onClick={mode === 'list' ? onClose : resetToList}
            className={`${theme.borderRadius} p-1 ${theme.labelText} transition ${theme.transitionSpeed} ${theme.taskHover}`}
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className={`max-h-[60vh] overflow-y-auto px-5 py-4`}>
          {mode === 'list' && (
            <>
              {categories.length === 0 && (
                <div className="py-6 text-center">
                  <Tag className={`mx-auto h-8 w-8 ${theme.emptyIcon}`} />
                  <p className={`mt-2 text-sm ${theme.emptyText}`}>No categories yet</p>
                  <p className={`mt-1 text-xs ${theme.emptyText} opacity-70`}>Create one to organize your tasks</p>
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
                    theme={theme}
                  />
                ))}
              </ul>

              <button
                type="button"
                onClick={openCreate}
                className={`mt-3 flex w-full items-center justify-center gap-1.5 ${theme.borderRadius} ${theme.decorativeBorder} border-dashed ${theme.inputBorder} py-2.5 text-sm font-medium ${theme.sidebarItemText} transition ${theme.transitionSpeed} ${theme.taskHover}`}
              >
                <span>+</span> New Category
              </button>
            </>
          )}

          {(mode === 'create' || mode === 'edit') && (
            <form onSubmit={mode === 'create' ? handleCreate : handleUpdate}>
              <div className="mb-4">
                <label className={`mb-1 block text-[11px] font-medium uppercase tracking-wide ${theme.labelText} ${theme.labelStyle}`}>
                  Name *
                </label>
                <input
                  autoFocus
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Category name"
                  maxLength={100}
                  className={`w-full ${theme.borderRadius} border ${theme.inputBorder} px-3 py-2 text-sm ${theme.inputText} ${theme.inputBg} outline-none ${theme.inputPlaceholder} ${theme.inputFocus}`}
                />
              </div>

              <div className="mb-4">
                <label className={`mb-1.5 block text-[11px] font-medium uppercase tracking-wide ${theme.labelText} ${theme.labelStyle}`}>
                  Icon
                </label>
                <div className="flex flex-wrap gap-1.5">
                  {PRESET_CATEGORY_ICONS.map(({ key, Icon, label }) => (
                    <button
                      key={key}
                      type="button"
                      onClick={() => setIcon(icon === key ? '' : key)}
                      title={label}
                      className={`flex h-8 w-8 items-center justify-center ${theme.borderRadius} border transition ${theme.transitionSpeed} ${
                        icon === key
                          ? `${theme.inputBorder} ${theme.sidebarItemActive}`
                          : `${theme.inputBorder} ${theme.taskHover}`
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
                    className={`mt-1 text-[10px] ${theme.labelText} hover:opacity-80`}
                  >
                    Clear icon
                  </button>
                )}
              </div>

              <div className="mb-4">
                <label className={`mb-1.5 block text-[11px] font-medium uppercase tracking-wide ${theme.labelText} ${theme.labelStyle}`}>
                  Color
                </label>
                <div className="flex flex-wrap gap-1.5">
                  {PRESET_COLORS.map((c) => (
                    <button
                      key={c}
                      type="button"
                      onClick={() => setColor(c)}
                      className={`h-7 w-7 rounded-full border-2 transition ${theme.transitionSpeed} ${
                        color === c ? `${theme.inputBorder} scale-110` : 'border-transparent hover:scale-105'
                      }`}
                      style={{ backgroundColor: c }}
                    />
                  ))}
                </div>
                <div className="mt-2 flex items-center gap-2">
                  <span
                    className={`h-5 w-5 rounded-full border ${theme.inputBorder}`}
                    style={{ backgroundColor: color }}
                  />
                  <input
                    type="text"
                    value={color}
                    onChange={(e) => setColor(e.target.value.toUpperCase())}
                    placeholder="#RRGGBB"
                    maxLength={7}
                    className={`w-24 ${theme.borderRadius} border ${theme.inputBorder} ${theme.inputBg} px-2 py-1 text-xs ${theme.inputText} outline-none ${theme.inputFocus}`}
                  />
                </div>
              </div>

              {error && (
                <p className={`mb-3 ${theme.borderRadius} bg-red-50 px-3 py-2 text-xs text-red-600`}>{error}</p>
              )}

              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={saving || !name.trim()}
                  className={`flex-1 ${theme.borderRadius} ${theme.btnPrimary} px-4 py-2 text-sm font-medium ${theme.btnPrimaryText} transition ${theme.transitionSpeed} ${theme.btnPrimaryHover} disabled:opacity-40`}
                >
                  {saving ? 'Saving...' : mode === 'create' ? 'Create' : 'Save Changes'}
                </button>
                <button
                  type="button"
                  onClick={resetToList}
                  className={`${theme.borderRadius} border ${theme.inputBorder} px-4 py-2 text-sm font-medium ${theme.taskText} transition ${theme.transitionSpeed} ${theme.taskHover}`}
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
  theme,
}: {
  category: CategoryResponse;
  index: number;
  total: number;
  reordering: boolean;
  onEdit: () => void;
  onDelete: () => void;
  onMoveUp: () => void;
  onMoveDown: () => void;
  theme: ThemeColors;
}) {
  const [confirmDelete, setConfirmDelete] = useState(false);

  return (
    <li className={`group ${theme.borderRadius} border ${theme.sidebarDivider} px-3 py-2.5 transition ${theme.transitionSpeed}`}>
      <div className="flex items-center gap-3">
        <div className={`flex h-8 w-8 shrink-0 items-center justify-center ${theme.borderRadius} ${theme.sidebarItemActive}`}>
          {category.color ? (
            <span
              className="h-4 w-4 rounded-full"
              style={{ backgroundColor: category.color }}
            />
          ) : (
            createElement(getCategoryIcon(category.icon), { className: `h-4 w-4 ${theme.sidebarItemText}` })
          )}
        </div>

        <div className="min-w-0 flex-1">
          <p className={`truncate text-sm font-medium ${theme.taskText}`}>{category.name}</p>
          <p className={`text-[10px] ${theme.labelText}`}>
            {category.taskCount} task{category.taskCount !== 1 ? 's' : ''}
          </p>
        </div>

        <div className={`flex shrink-0 flex-col gap-0.5 opacity-0 transition ${theme.transitionSpeed} group-hover:opacity-100`}>
          <button
            type="button"
            onClick={onMoveUp}
            disabled={index === 0 || reordering}
            className={`rounded p-0.5 ${theme.labelText} transition ${theme.transitionSpeed} ${theme.taskHover} disabled:opacity-30`}
            title="Move up"
          >
            <ChevronUp className="h-3 w-3" />
          </button>
          <button
            type="button"
            onClick={onMoveDown}
            disabled={index === total - 1 || reordering}
            className={`rounded p-0.5 ${theme.labelText} transition ${theme.transitionSpeed} ${theme.taskHover} disabled:opacity-30`}
            title="Move down"
          >
            <ChevronDown className="h-3 w-3" />
          </button>
        </div>

        <button
          type="button"
          onClick={onEdit}
          className={`shrink-0 ${theme.borderRadius} p-1.5 ${theme.labelText} opacity-0 transition ${theme.transitionSpeed} ${theme.taskHover} group-hover:opacity-100`}
          title="Edit"
        >
          <Pencil className="h-3.5 w-3.5" />
        </button>

        {confirmDelete ? (
          <div className="flex shrink-0 items-center gap-1">
            <button
              type="button"
              onClick={() => { onDelete(); setConfirmDelete(false); }}
              className={`${theme.borderRadius} bg-red-600 px-2 py-1 text-[10px] font-medium text-white transition ${theme.transitionSpeed} hover:bg-red-700`}
            >
              Delete
            </button>
            <button
              type="button"
              onClick={() => setConfirmDelete(false)}
              className={`${theme.borderRadius} px-1.5 py-1 text-[10px] ${theme.labelText} transition ${theme.transitionSpeed} ${theme.taskHover}`}
            >
              Cancel
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => setConfirmDelete(true)}
            className={`shrink-0 ${theme.borderRadius} p-1.5 ${theme.labelText} opacity-0 transition ${theme.transitionSpeed} hover:bg-red-50 hover:text-red-500 group-hover:opacity-100`}
            title="Delete"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        )}
      </div>
    </li>
  );
}
