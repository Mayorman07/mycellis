import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAllOrganizations } from '../lib/api/superAdmin';
import type { ApiError } from '../lib/api/client';
import type { SuperAdminOrgSummary } from '../lib/types';
import { SuperAdminBanner } from '../components/superadmin/SuperAdminBanner';

const GRID_COLS = 'grid-cols-[2fr_1.5fr_1fr_0.7fr_0.7fr_1fr]';
const HEADER_LABELS = ['Name', 'Slug', 'Plan', 'Members', 'Stalks', 'Created'];

const PLAN_PILL_CLASSES: Record<string, string> = {
  FREE: 'bg-surface-sunken text-ink-muted',
  PRO: 'bg-[color-mix(in_srgb,var(--color-brand)_25%,transparent)] text-ink',
  ENTERPRISE: 'bg-brand text-brand-fg',
};

function formatRelativeTime(iso: string): string {
  const now = Date.now();
  const then = new Date(iso).getTime();
  const seconds = Math.floor((now - then) / 1000);

  if (seconds < 60) return `${seconds}s ago`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

export default function SuperAdminOrganizationsPage() {
  const [search, setSearch] = useState('');

  const orgsQuery = useQuery<SuperAdminOrgSummary[], ApiError>({
    queryKey: ['super-admin', 'organizations'],
    queryFn: getAllOrganizations,
  });

  // Client-side filter over an already-fully-loaded, <100-row list — no
  // network call to throttle, so debouncing would only add perceived lag
  // for no benefit.
  const filtered = useMemo(() => {
    const orgs = orgsQuery.data ?? [];
    const term = search.trim().toLowerCase();
    if (!term) {
      return orgs;
    }
    return orgs.filter(
      (org) => org.name.toLowerCase().includes(term) || org.slug.toLowerCase().includes(term),
    );
  }, [orgsQuery.data, search]);

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-5xl mx-auto px-6 py-12">
        <div className="flex items-center justify-between mb-3">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">
            MYCELLIS · SUPER ADMIN · ORGANIZATIONS
          </p>
          <SuperAdminBanner />
        </div>
        <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-8">
          All organizations.
        </h1>

        <input
          type="text"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search by name or slug…"
          className="w-full max-w-sm rounded-md border-[1.5px] border-[color-mix(in_srgb,var(--color-ink)_25%,transparent)] bg-surface-sunken px-4 py-2.5 text-sm text-ink placeholder:text-ink-subtle mb-6 focus:outline-none focus:border-brand focus:[box-shadow:0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]"
        />

        {orgsQuery.isPending ? (
          <TableSkeleton />
        ) : orgsQuery.isError ? (
          <ErrorState onRetry={() => orgsQuery.refetch()} />
        ) : (
          <div className="rounded-lg border border-hairline bg-surface-raised overflow-hidden overflow-x-auto">
            <div className={`grid ${GRID_COLS} gap-4 py-3 px-4 border-b border-hairline min-w-[640px]`}>
              {HEADER_LABELS.map((label) => (
                <span key={label} className="font-mono text-xs uppercase tracking-wider text-ink-subtle">
                  {label}
                </span>
              ))}
            </div>

            {filtered.length === 0 ? (
              <p className="text-center text-ink-muted py-8">No organizations yet.</p>
            ) : (
              filtered.map((org) => (
                <div
                  key={org.id}
                  className={`grid ${GRID_COLS} items-center gap-4 py-3 px-4 border-b border-hairline last:border-b-0 min-w-[640px]`}
                >
                  <Link
                    to={`/super-admin/organizations/${org.id}/stalks`}
                    className="text-sm text-ink hover:underline truncate"
                  >
                    {org.name}
                  </Link>
                  <span className="font-mono text-xs text-ink-muted truncate">{org.slug}</span>
                  <span
                    className={`inline-flex items-center rounded-full px-2.5 py-0.5 font-mono text-[11px] uppercase tracking-wider w-fit ${
                      PLAN_PILL_CLASSES[org.planTier] ?? 'bg-surface-sunken text-ink-muted'
                    }`}
                  >
                    {org.planTier}
                  </span>
                  <span className="font-mono text-sm text-ink-muted">{org.memberCount}</span>
                  <span className="font-mono text-sm text-ink-muted">{org.stalkCount}</span>
                  <span className="font-mono text-xs text-ink-subtle">
                    {formatRelativeTime(org.createdAt)}
                  </span>
                </div>
              ))
            )}
          </div>
        )}
      </main>
    </div>
  );
}

function TableSkeleton() {
  return (
    <div className="rounded-lg border border-hairline bg-surface-raised overflow-hidden">
      {[0, 1, 2, 3].map((row) => (
        <div key={row} className="flex items-center gap-4 py-3 px-4 border-b border-hairline last:border-b-0">
          <span className="w-32 h-3.5 rounded bg-surface-sunken" />
          <span className="w-20 h-3.5 rounded bg-surface-sunken" />
          <span className="w-14 h-3.5 rounded bg-surface-sunken" />
          <span className="w-8 h-3.5 rounded bg-surface-sunken" />
        </div>
      ))}
    </div>
  );
}

function ErrorState({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="rounded-lg border border-hairline bg-surface-raised p-8 text-center">
      <p className="text-sm text-ink-muted mb-4">Something went wrong loading this data.</p>
      <button
        type="button"
        onClick={onRetry}
        className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-brand-fg"
      >
        Try again
      </button>
    </div>
  );
}
