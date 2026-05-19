import { useState } from 'react';
import type { ContextTheme } from '../types';
import { PRESET_CONTEXT_ICONS } from '../utils/icons';

interface CreateContextModalProps {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
  onSubmit: (data: { name: string; theme: ContextTheme; icon: string }) => Promise<void>;
}

const THEMES: { value: ContextTheme; label: string; colors: string }[] = [
  { value: 'MINIMALIST', label: 'Minimalist', colors: 'bg-stone-100 border-stone-300 text-stone-700' },
  { value: 'DESIGN', label: 'Design', colors: 'bg-violet-50 border-violet-300 text-violet-700' },
  { value: 'FORMAL', label: 'Formal', colors: 'bg-slate-100 border-slate-300 text-slate-700' },
  { value: 'NATURE', label: 'Nature', colors: 'bg-emerald-50 border-emerald-300 text-emerald-700' },
  { value: 'DARK', label: 'Dark', colors: 'bg-zinc-800 border-zinc-600 text-zinc-100' },
  { value: 'DRAGONS', label: 'Dragons', colors: 'bg-stone-900 border-amber-700 text-amber-100' },
  { value: 'ICE_DRAGONS', label: 'Ice Dragons', colors: 'bg-slate-900 border-cyan-700 text-cyan-100' },
];

export default function CreateContextModal({ open, onClose, onCreated, onSubmit }: CreateContextModalProps) {
  const [name, setName] = useState('');
  const [theme, setTheme] = useState<ContextTheme>('MINIMALIST');
  const [icon, setIcon] = useState('briefcase');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!open) return null;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');

    if (!name.trim()) {
      setError('Name is required');
      return;
    }

    setSubmitting(true);
    try {
      await onSubmit({ name: name.trim(), theme, icon });
      setName('');
      setTheme('MINIMALIST');
      setIcon('briefcase');
      onCreated();
      onClose();
    } catch (err: unknown) {
      const msg =
        err && typeof err === 'object' && 'response' in err
          ? (err as { response?: { data?: { message?: string } } }).response?.data?.message
          : undefined;
      setError(msg || 'Failed to create context');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative w-full max-w-md rounded-xl border border-stone-200 bg-white p-6 shadow-xl">
        <h2 className="mb-5 text-lg font-semibold text-stone-900">Create Context</h2>

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-stone-700">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g. Work, Personal, Studies"
              className="w-full rounded-lg border border-stone-300 px-3 py-2 text-sm text-stone-900 placeholder:text-stone-400 focus:border-stone-500 focus:outline-none focus:ring-1 focus:ring-stone-500"
              maxLength={100}
              autoFocus
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-stone-700">Icon</label>
            <div className="flex flex-wrap gap-2">
              {PRESET_CONTEXT_ICONS.map(({ key, Icon, label }) => (
                <button
                  key={key}
                  type="button"
                  onClick={() => setIcon(key)}
                  title={label}
                  className={`flex h-9 w-9 items-center justify-center rounded-lg border transition ${
                    icon === key
                      ? 'border-stone-900 bg-stone-100 ring-1 ring-stone-900'
                      : 'border-stone-200 hover:border-stone-400'
                  }`}
                >
                  <Icon className="h-4.5 w-4.5" />
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-stone-700">Theme</label>
            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
              {THEMES.map((t) => (
                <button
                  key={t.value}
                  type="button"
                  onClick={() => setTheme(t.value)}
                  className={`rounded-lg border px-3 py-2 text-xs font-medium transition ${t.colors} ${
                    theme === t.value ? 'ring-2 ring-stone-900 ring-offset-1' : ''
                  }`}
                >
                  {t.label}
                </button>
              ))}
            </div>
          </div>

          {error && <p className="text-sm text-red-600">{error}</p>}

          <div className="flex items-center justify-end gap-3 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg px-4 py-2 text-sm text-stone-600 transition hover:bg-stone-100"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="rounded-lg bg-stone-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-stone-800 disabled:opacity-50"
            >
              {submitting ? 'Creating...' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
