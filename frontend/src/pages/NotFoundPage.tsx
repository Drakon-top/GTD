import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gray-50">
      <h1 className="text-6xl font-bold text-gray-200">404</h1>
      <p className="mt-2 text-gray-500">Page not found</p>
      <Link
        to="/"
        className="mt-4 text-sm font-medium text-gray-900 underline"
      >
        Go home
      </Link>
    </div>
  );
}
