import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSession } from '../lib/hooks/useSession';
import { useDashboardData } from '../lib/hooks/useDashboardData';
import { DashboardHeader } from '../components/dashboard/DashboardHeader';
import { DashboardHero } from '../components/dashboard/DashboardHero';
import { KpiStrip } from '../components/dashboard/KpiStrip';
import { StalkTable } from '../components/dashboard/StalkTable';
import { EmptyDashboard } from '../components/dashboard/EmptyDashboard';
import { LoadingDashboard } from '../components/dashboard/LoadingDashboard';
import { ErrorDashboard } from '../components/dashboard/ErrorDashboard';

const INITIAL_LOAD_GRACE_MS = 300;

export default function DashboardPage() {
  const navigate = useNavigate();
  const session = useSession();
  const { stalksQuery, pulsesQuery } = useDashboardData();

  // Avoids a skeleton flash on fast networks: the skeleton only becomes eligible
  // to render once we've waited past this grace window, not from first render.
  const [pastInitialGracePeriod, setPastInitialGracePeriod] = useState(false);
  useEffect(() => {
    const id = setTimeout(() => setPastInitialGracePeriod(true), INITIAL_LOAD_GRACE_MS);
    return () => clearTimeout(id);
  }, []);

  const orgName = session.data?.organization.name ?? '';
  const planTier = session.data?.organization.planTier ?? 'FREE';
  const userInitials = session.data
    ? `${session.data.user.firstName.charAt(0)}${session.data.user.lastName.charAt(0)}`.toUpperCase()
    : '';

  const stalks = stalksQuery.data?.content ?? [];
  const showLoadingSkeleton =
    pastInitialGracePeriod && stalksQuery.isFetching && stalksQuery.data === undefined;

  function renderContent() {
    if (stalksQuery.isError || pulsesQuery.isError) {
      return (
        <ErrorDashboard
          error={stalksQuery.error ?? pulsesQuery.error}
          onRetry={() => {
            stalksQuery.refetch();
            pulsesQuery.refetch();
          }}
        />
      );
    }

    if (showLoadingSkeleton) {
      return <LoadingDashboard />;
    }

    if (stalksQuery.data && stalks.length === 0) {
      return (
        <EmptyDashboard orgName={orgName} onCreateStalk={() => navigate('/stalks/new')} />
      );
    }

    return (
      <>
        <DashboardHero
          key={stalksQuery.dataUpdatedAt}
          stalksCount={stalks.length}
          lastSyncedAt={stalksQuery.dataUpdatedAt}
        />
        <KpiStrip stalks={stalks} />
        <div className="mt-8">
          <StalkTable stalks={stalks} pulsesByStalkId={pulsesQuery.data?.pulsesByStalkId ?? {}} />
        </div>
      </>
    );
  }

  return (
    <div className="min-h-screen bg-surface">
      <DashboardHeader orgName={orgName} planTier={planTier} userInitials={userInitials} />
      <main className="max-w-6xl mx-auto px-6 py-8">{renderContent()}</main>
    </div>
  );
}
