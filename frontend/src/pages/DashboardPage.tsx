import { useQuery } from '@tanstack/react-query';
import { useSession } from '../lib/hooks/useSession';
import { listStalks } from '../lib/api/stalks';
import { DashboardHeader } from '../components/dashboard/DashboardHeader';
import { DashboardHero } from '../components/dashboard/DashboardHero';
import { KpiStrip } from '../components/dashboard/KpiStrip';

export default function DashboardPage() {
  const session = useSession();
  const stalksQuery = useQuery({
    queryKey: ['stalks'],
    queryFn: () => listStalks(),
  });

  if (session.isLoading || stalksQuery.isLoading) {
    return <div className="text-ink-muted p-8">Loading...</div>;
  }

  // session.isError is already handled by ProtectedRoute — should not reach here.

  const orgName = session.data?.organization.name ?? '';
  const planTier = session.data?.organization.planTier ?? 'FREE';
  const userInitials = session.data
    ? `${session.data.user.firstName.charAt(0)}${session.data.user.lastName.charAt(0)}`.toUpperCase()
    : '';
  const stalks = stalksQuery.data?.content ?? [];

  return (
    <div className="min-h-screen bg-surface">
      <DashboardHeader orgName={orgName} planTier={planTier} userInitials={userInitials} />
      <main className="max-w-6xl mx-auto px-6 py-8">
        {stalksQuery.isError ? (
          <p className="text-ink-muted">Failed to load stalks. Refresh to try again.</p>
        ) : (
          <>
            <DashboardHero
              key={stalksQuery.dataUpdatedAt}
              stalksCount={stalks.length}
              lastSyncedAt={stalksQuery.dataUpdatedAt}
            />
            <KpiStrip stalks={stalks} />
            {/* TEMPORARY: placeholder for stalk list. Replaced in Commit 3. */}
            <div className="mt-8 border-t border-hairline pt-6">
              <p className="text-sm text-ink-muted">Stalk list arrives in Commit 3.</p>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
