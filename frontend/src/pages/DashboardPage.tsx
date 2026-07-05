import { useSession } from '../lib/hooks/useSession';
import { useDashboardData } from '../lib/hooks/useDashboardData';
import { DashboardHeader } from '../components/dashboard/DashboardHeader';
import { DashboardHero } from '../components/dashboard/DashboardHero';
import { KpiStrip } from '../components/dashboard/KpiStrip';
import { StalkTable } from '../components/dashboard/StalkTable';

export default function DashboardPage() {
  const session = useSession();
  const { stalks, pulsesByStalkId, isLoading, isError, lastSyncedAt } = useDashboardData();

  if (session.isLoading || isLoading) {
    return <div className="text-ink-muted p-8">Loading...</div>;
  }

  // session.isError is already handled by ProtectedRoute — should not reach here.

  const orgName = session.data?.organization.name ?? '';
  const planTier = session.data?.organization.planTier ?? 'FREE';
  const userInitials = session.data
    ? `${session.data.user.firstName.charAt(0)}${session.data.user.lastName.charAt(0)}`.toUpperCase()
    : '';

  return (
    <div className="min-h-screen bg-surface">
      <DashboardHeader orgName={orgName} planTier={planTier} userInitials={userInitials} />
      <main className="max-w-6xl mx-auto px-6 py-8">
        {isError ? (
          <p className="text-ink-muted">Failed to load stalks. Refresh to try again.</p>
        ) : (
          <>
            <DashboardHero key={lastSyncedAt} stalksCount={stalks.length} lastSyncedAt={lastSyncedAt} />
            <KpiStrip stalks={stalks} />
            <div className="mt-8">
              <StalkTable stalks={stalks} pulsesByStalkId={pulsesByStalkId} />
            </div>
          </>
        )}
      </main>
    </div>
  );
}
