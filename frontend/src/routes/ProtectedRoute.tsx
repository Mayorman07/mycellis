import { Navigate, Outlet } from 'react-router-dom';
import { useSession } from '../lib/hooks/useSession';

export function ProtectedRoute() {
  const { isLoading, isError, error } = useSession();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 rounded-full border-2 border-hairline border-t-ink-subtle animate-spin" />
      </div>
    );
  }

  if (isError) {
    if (error?.status === 401) {
      return <Navigate to="/login" replace />;
    }

    return (
      <div className="min-h-screen flex items-center justify-center text-ink-subtle">
        Something went wrong. Refresh to try again.
      </div>
    );
  }

  return <Outlet />;
}
