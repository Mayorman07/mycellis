import { useEffect, type ReactNode } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getPublicStatus } from '../lib/api/status';
import type { ApiError } from '../lib/api/client';
import type { OverallState, PublicStalkStatus, PublicStatusResponse } from '../lib/types';
import { getTheme, setTheme } from '../lib/theme';
import { AmbientNetwork } from '../components/auth/AmbientNetwork';
import { StatusDot } from '../components/dashboard/StatusDot';

const REFETCH_INTERVAL_MS = 30_000;
const UPTIME_WINDOW_DAYS = 90;

function formatRelativeTime(iso: string): string {
  const now = Date.now();
  const then = new Date(iso).getTime();
  const seconds = Math.floor((now - then) / 1000);

  if (seconds < 60) return `${seconds}s ago`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

const OVERALL_STATE_COPY: Record<
  OverallState,
  { sentence: string; colorClass: string; subtitle: string; subtitleColorClass: string }
> = {
  HEALTHY: {
    sentence: 'The ecosystem is healthy.',
    colorClass: 'text-state-healthy',
    subtitle: 'All services are operating normally.',
    subtitleColorClass: 'text-ink-muted',
  },
  STRESSED: {
    sentence: 'The ecosystem is under stress.',
    colorClass: 'text-state-stressed',
    subtitle: 'Some services are experiencing elevated latency.',
    subtitleColorClass: 'text-state-stressed',
  },
  DEGRADED: {
    sentence: 'The ecosystem is degraded.',
    colorClass: 'text-state-stressed',
    subtitle: 'Some services are running slowly or failing.',
    subtitleColorClass: 'text-state-stressed',
  },
  IMPAIRED: {
    sentence: 'The ecosystem is impaired.',
    colorClass: 'text-state-down',
    subtitle: 'One or more services are unavailable.',
    subtitleColorClass: 'text-state-down',
  },
};

function uptimeBarColorClass(value: number | null): string {
  if (value === null) return 'bg-surface-sunken';
  if (value >= 99) return 'bg-state-healthy';
  if (value >= 95) return 'bg-state-stressed';
  return 'bg-state-down';
}

export default function PublicStatusPage() {
  const { slug } = useParams<{ slug: string }>();

  // Same locked-cream, per-page mount/unmount pattern as the auth pages —
  // this is a first-impression public brand surface, session-agnostic, so
  // it ignores whatever theme a logged-in visitor last set for themselves.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  const statusQuery = useQuery<PublicStatusResponse, ApiError>({
    queryKey: ['status', slug],
    queryFn: () => getPublicStatus(slug!),
    enabled: !!slug,
    refetchInterval: REFETCH_INTERVAL_MS,
    retry: 1,
  });

  if (statusQuery.isPending) {
    return (
      <PageShell align="center">
        <BrandBlock />
        <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-3">
          Loading status…
        </h1>
        <ComponentsSkeleton />
      </PageShell>
    );
  }

  if (statusQuery.isError) {
    if (statusQuery.error.status === 404) {
      return (
        <PageShell align="center">
          <BrandBlock />
          <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-3">
            Nothing to see here.
          </h1>
          <p className="text-ink-muted leading-[1.5]">
            This workspace doesn't have a public status page.
          </p>
        </PageShell>
      );
    }

    // Never surface internal error details on a public, unauthenticated page.
    return (
      <PageShell align="center">
        <BrandBlock />
        <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-3">
          Couldn't load status.
        </h1>
        <p className="text-ink-muted leading-[1.5] mb-6">
          Something went wrong on our end. Try again in a moment.
        </p>
        <button
          type="button"
          onClick={() => statusQuery.refetch()}
          className="rounded-md border border-hairline-strong bg-surface-raised px-6 py-3 text-sm font-medium text-ink hover:bg-surface-sunken"
        >
          Try again
        </button>
      </PageShell>
    );
  }

  const status = statusQuery.data;

  // Distinct from "all stalks are DORMANT" below — this is a workspace that
  // has never set up any monitoring at all.
  if (status.stalks.length === 0) {
    return (
      <PageShell align="center">
        <BrandBlock orgName={status.organization.name} />
        <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-3">
          Nothing being watched yet.
        </h1>
        <p className="text-ink-muted leading-[1.5]">
          This workspace hasn't set up any monitoring.
        </p>
      </PageShell>
    );
  }

  // DORMANT means the workspace owner paused monitoring — publishing "paused"
  // publicly isn't the intent of a status page, so those components are
  // simply not shown here (not shown as "unknown" or anything else).
  const visibleStalks = status.stalks.filter((stalk) => stalk.reliabilityState !== 'DORMANT');
  const overall = OVERALL_STATE_COPY[status.overallState];

  return (
    <PageShell align="top">
      <BrandBlock orgName={status.organization.name} />

      <div className="mt-12">
        <h1 className={`font-display font-normal text-[36px] leading-tight tracking-tight mb-2 ${overall.colorClass}`}>
          {overall.sentence}
        </h1>
        <p className={`font-mono text-sm mb-2 ${overall.subtitleColorClass}`}>{overall.subtitle}</p>
        <p className="font-mono text-xs text-ink-subtle">
          Last updated {formatRelativeTime(status.lastUpdated)}
        </p>
      </div>

      <div className="mt-12">
        <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
          Components
        </p>
        <div className="rounded-lg border border-hairline bg-surface-raised overflow-hidden">
          {visibleStalks.length === 0 ? (
            <p className="text-center text-ink-muted py-8">No components being monitored yet.</p>
          ) : (
            visibleStalks.map((stalk, index) => <ComponentRow key={index} stalk={stalk} />)
          )}
        </div>
      </div>

      <div className="mt-12">
        <p className="font-mono text-sm text-ink-muted mb-1">Powered by Mycellis</p>
        <p className="font-mono text-xs text-ink-subtle">
          Coming soon: incident history · email subscriptions · webhooks
        </p>
      </div>
    </PageShell>
  );
}

function BrandBlock({ orgName }: { orgName?: string }) {
  return (
    <div className="mb-3">
      <p className="font-mono uppercase tracking-widest text-sm font-semibold text-ink mb-1">
        MYCELLIS · STATUS
      </p>
      {orgName ? (
        <>
          <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-2">
            {orgName}
          </h1>
          <p className="font-mono text-sm text-ink-muted">Digital ecosystem status</p>
        </>
      ) : (
        <p className="font-mono text-sm text-ink-muted">Digital ecosystem status</p>
      )}
    </div>
  );
}

function ComponentRow({ stalk }: { stalk: PublicStalkStatus }) {
  return (
    <div className="flex flex-wrap items-center gap-4 py-4 px-6 border-b border-hairline last:border-b-0">
      <div className="flex items-center gap-3 min-w-0 flex-shrink-0">
        <StatusDot reliabilityState={stalk.reliabilityState} size={12} />
        <span className="text-sm text-ink truncate">{stalk.nickname}</span>
      </div>

      <div className="flex-1 min-w-[240px]">
        <div className="overflow-x-auto">
          <UptimeBarStrip history={stalk.uptimeHistory90d} />
        </div>
        <div className="flex justify-between mt-1.5">
          <span className="font-mono text-[10px] tracking-wider text-ink-subtle">
            {UPTIME_WINDOW_DAYS} DAYS AGO
          </span>
          <span className="font-mono text-[10px] tracking-wider text-ink-subtle">TODAY</span>
        </div>
      </div>

      <span className="font-mono text-sm text-ink-muted flex-shrink-0 ml-auto">
        {stalk.healthIndex.toFixed(2)}%
      </span>
    </div>
  );
}

function UptimeBarStrip({ history }: { history: (number | null)[] }) {
  return (
    <div className="flex items-end gap-[1px] h-6">
      {history.map((value, index) => (
        <span
          key={index}
          className={`w-[3px] h-6 rounded-[1px] ${uptimeBarColorClass(value)}`}
        />
      ))}
    </div>
  );
}

function ComponentsSkeleton() {
  return (
    <div className="rounded-lg border border-hairline bg-surface-raised overflow-hidden">
      {[0, 1, 2].map((row) => (
        <div
          key={row}
          className="flex items-center justify-between gap-4 py-4 px-6 border-b border-hairline last:border-b-0"
        >
          <div className="flex items-center gap-3">
            <span className="w-3 h-3 rounded-full bg-surface-sunken" />
            <span className="w-28 h-3.5 rounded bg-surface-sunken" />
          </div>
          <span className="flex-1 h-6 rounded bg-surface-sunken" />
          <span className="w-12 h-3.5 rounded bg-surface-sunken" />
        </div>
      ))}
    </div>
  );
}

function PageShell({ children, align }: { children: ReactNode; align: 'center' | 'top' }) {
  return (
    <div className="h-screen overflow-y-auto scrollbar-none flex bg-surface">
      <div
        className={`w-full lg:w-[60%] relative flex flex-col px-8 sm:px-16 bg-[radial-gradient(ellipse_at_center,transparent_0%,color-mix(in_srgb,var(--color-ink)_2%,transparent)_100%)] ${
          align === 'center' ? 'justify-center py-12' : 'pt-16 pb-16'
        }`}
      >
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-1/2 bg-[linear-gradient(135deg,color-mix(in_srgb,var(--color-brand)_4%,transparent)_0%,transparent_60%)]"
        />
        <div
          className={`relative w-full mx-auto lg:mx-0 ${align === 'center' ? 'max-w-md' : 'max-w-2xl'}`}
        >
          {children}
        </div>
      </div>

      <div className="hidden lg:flex lg:w-[40%] sticky top-0 h-screen overflow-hidden">
        <AmbientNetwork className="w-full h-full" />
      </div>
    </div>
  );
}
