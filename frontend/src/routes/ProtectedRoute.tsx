import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useSession } from '../lib/hooks/useSession';

// Never capture these as a return target — capturing them would send a user
// straight back to the page that redirected them here in the first place.
const NO_REDIRECT_BACK_PATHS = new Set(['/login', '/signup']);

export function ProtectedRoute() {
  const { isLoading, isError, error } = useSession();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-8 h-8 rounded-full border-2 border-hairline border-t-ink-subtle animate-spin" />
      </div>
    );
  }

  if (isError) {
    if (error?.status === 401) {
      const from = location.pathname + location.search;
      const state = NO_REDIRECT_BACK_PATHS.has(location.pathname) ? undefined : { from };
      return <Navigate to="/login" replace state={state} />;
    }

    return (
      <div className="min-h-screen flex items-center justify-center text-ink-subtle">
        Something went wrong. Refresh to try again.
      </div>
    );
  }

  return <Outlet />;
}
