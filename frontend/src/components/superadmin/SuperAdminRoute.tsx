import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useSession } from '../../lib/hooks/useSession';

export function SuperAdminRoute({ children }: { children: ReactNode }) {
  const session = useSession();

  if (session.isLoading) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center">
        <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">Loading…</p>
      </div>
    );
  }

  if (!session.data?.user.roles.includes('SUPER_ADMIN')) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
}
