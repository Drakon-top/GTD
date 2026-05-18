import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-stone-50">
      <h1 className="text-6xl font-bold text-stone-200">404</h1>
      <p className="mt-2 text-stone-500">Page not found</p>
      <Link
        to="/"
        className="mt-4 text-sm font-medium text-stone-900 underline decoration-stone-300 underline-offset-2 transition hover:decoration-stone-900"
      >
        Go home
      </Link>
    </div>
  );
}
