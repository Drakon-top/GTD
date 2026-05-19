import { type FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { Check } from 'lucide-react';
import apiClient from '../api/client';
import type { ErrorResponse } from '../types';

export default function RegisterPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setFieldErrors({});

    if (password !== confirm) {
      setFieldErrors({ confirm: 'Passwords do not match' });
      return;
    }

    setLoading(true);
    try {
      await apiClient.post('/auth/register', { email, password });
      setSuccess(true);
      setTimeout(() => navigate('/login', { replace: true }), 1500);
    } catch (err) {
      if (isAxiosError<ErrorResponse>(err) && err.response?.data) {
        const body = err.response.data;
        setError(body.message ?? 'Registration failed');
        if (body.details) setFieldErrors(body.details);
      } else {
        setError('Unable to connect. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  }

  if (success) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-stone-50 px-4">
        <div className="text-center">
          <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-emerald-100">
            <Check className="h-6 w-6 text-emerald-600" />
          </div>
          <h2 className="text-lg font-semibold text-stone-900">
            Account created!
          </h2>
          <p className="mt-1 text-sm text-stone-500">
            Redirecting to sign in...
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-stone-50 px-4">
      <div className="w-full max-w-[360px]">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-stone-900 text-lg font-bold text-white">
            G
          </div>
          <h1 className="text-2xl font-semibold tracking-tight text-stone-900">
            Create your account
          </h1>
          <p className="mt-1 text-sm text-stone-500">
            Start organizing with GTD
          </p>
        </div>

        <div className="rounded-xl border border-stone-200 bg-white p-6 shadow-sm">
          {error && (
            <div className="mb-4 rounded-lg bg-red-50 px-3 py-2.5 text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label
                htmlFor="email"
                className="mb-1.5 block text-sm font-medium text-stone-700"
              >
                Email
              </label>
              <input
                id="email"
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={`w-full rounded-lg border px-3 py-2 text-sm outline-none transition placeholder:text-stone-400 focus:ring-2 focus:ring-stone-900/10 ${
                  fieldErrors.email
                    ? 'border-red-300 focus:border-red-400'
                    : 'border-stone-200 focus:border-stone-400'
                }`}
                placeholder="you@example.com"
              />
              {fieldErrors.email && (
                <p className="mt-1 text-xs text-red-600">{fieldErrors.email}</p>
              )}
            </div>

            <div>
              <label
                htmlFor="password"
                className="mb-1.5 block text-sm font-medium text-stone-700"
              >
                Password
              </label>
              <input
                id="password"
                type="password"
                required
                autoComplete="new-password"
                minLength={8}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={`w-full rounded-lg border px-3 py-2 text-sm outline-none transition placeholder:text-stone-400 focus:ring-2 focus:ring-stone-900/10 ${
                  fieldErrors.password
                    ? 'border-red-300 focus:border-red-400'
                    : 'border-stone-200 focus:border-stone-400'
                }`}
                placeholder="Min. 8 characters"
              />
              {fieldErrors.password && (
                <p className="mt-1 text-xs text-red-600">
                  {fieldErrors.password}
                </p>
              )}
            </div>

            <div>
              <label
                htmlFor="confirm"
                className="mb-1.5 block text-sm font-medium text-stone-700"
              >
                Confirm password
              </label>
              <input
                id="confirm"
                type="password"
                required
                autoComplete="new-password"
                minLength={8}
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                className={`w-full rounded-lg border px-3 py-2 text-sm outline-none transition placeholder:text-stone-400 focus:ring-2 focus:ring-stone-900/10 ${
                  fieldErrors.confirm
                    ? 'border-red-300 focus:border-red-400'
                    : 'border-stone-200 focus:border-stone-400'
                }`}
                placeholder="Repeat your password"
              />
              {fieldErrors.confirm && (
                <p className="mt-1 text-xs text-red-600">
                  {fieldErrors.confirm}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-lg bg-stone-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-stone-800 active:bg-stone-950 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? (
                <span className="inline-flex items-center gap-2">
                  <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                  Creating account...
                </span>
              ) : (
                'Create account'
              )}
            </button>
          </form>
        </div>

        <p className="mt-5 text-center text-sm text-stone-500">
          Already have an account?{' '}
          <Link
            to="/login"
            className="font-medium text-stone-900 underline decoration-stone-300 underline-offset-2 transition hover:decoration-stone-900"
          >
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
