import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getAllUsers } from '../lib/api/superAdmin';
import type { ApiError } from '../lib/api/client';
import type { SuperAdminUserSummary } from '../lib/types';
import { SuperAdminBanner } from '../components/superadmin/SuperAdminBanner';

const GRID_COLS = 'grid-cols-[1.5fr_1.2fr_1.2fr_1fr_0.8fr_1fr]';
const HEADER_LABELS = ['Email', 'Name', 'Organization', 'Role', 'Status', 'Created'];

// Backend's Status enum has 5 values (NEW/ACTIVE/INACTIVE/DEACTIVATED/BLOCKED),
// not the 3-value ACTIVE/NEW/DISABLED set this page was originally scoped
// against — mapped all 5 to a sensible severity tint.
const STATUS_PILL_CLASSES: Record<string, string> = {
  ACTIVE: 'bg-[color-mix(in_srgb,var(--color-state-healthy)_15%,transparent)] text-state-healthy',
  NEW: 'bg-[color-mix(in_srgb,var(--color-state-stressed)_15%,transparent)] text-state-stressed',
  INACTIVE: 'bg-surface-sunken text-ink-muted',
  DEACTIVATED: 'bg-surface-sunken text-ink-muted',
  BLOCKED: 'bg-[color-mix(in_srgb,var(--color-state-down)_15%,transparent)] text-state-down',
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

export default function SuperAdminUsersPage() {
  const [search, setSearch] = useState('');

  const usersQuery = useQuery<SuperAdminUserSummary[], ApiError>({
    queryKey: ['super-admin', 'users'],
    queryFn: getAllUsers,
  });

  const filtered = useMemo(() => {
    const users = usersQuery.data ?? [];
    const term = search.trim().toLowerCase();
    if (!term) {
      return users;
    }
    return users.filter(
      (user) =>
        user.email.toLowerCase().includes(term) ||
        `${user.firstName} ${user.lastName}`.toLowerCase().includes(term),
    );
  }, [usersQuery.data, search]);

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-5xl mx-auto px-6 py-12">
        <div className="flex items-center justify-between mb-3">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">
            MYCELLIS · SUPER ADMIN · USERS
          </p>
          <SuperAdminBanner />
        </div>
        <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-8">
          All users.
        </h1>

        <input
          type="text"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search by email or name…"
          className="w-full max-w-sm rounded-md border-[1.5px] border-[color-mix(in_srgb,var(--color-ink)_25%,transparent)] bg-surface-sunken px-4 py-2.5 text-sm text-ink placeholder:text-ink-subtle mb-6 focus:outline-none focus:border-brand focus:[box-shadow:0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]"
        />

        {usersQuery.isPending ? (
          <TableSkeleton />
        ) : usersQuery.isError ? (
          <ErrorState onRetry={() => usersQuery.refetch()} />
        ) : (
          <div className="rounded-lg border border-hairline bg-surface-raised overflow-hidden overflow-x-auto">
            <div className={`grid ${GRID_COLS} gap-4 py-3 px-4 border-b border-hairline min-w-[720px]`}>
              {HEADER_LABELS.map((label) => (
                <span key={label} className="font-mono text-xs uppercase tracking-wider text-ink-subtle">
                  {label}
                </span>
              ))}
            </div>

            {filtered.length === 0 ? (
              <p className="text-center text-ink-muted py-8">No users yet.</p>
            ) : (
              filtered.map((user) => (
                <div
                  key={user.id}
                  className={`grid ${GRID_COLS} items-center gap-4 py-3 px-4 border-b border-hairline last:border-b-0 min-w-[720px]`}
                >
                  <span className="text-sm text-ink truncate">{user.email}</span>
                  <span className="text-sm text-ink-muted truncate">
                    {user.firstName} {user.lastName}
                  </span>
                  <span className="text-sm text-ink-muted truncate">
                    {user.organizationName ?? '—'}
                  </span>
                  <span className="font-mono text-xs text-ink-muted truncate">
                    {user.roles.join(', ') || '—'}
                  </span>
                  <span
                    className={`inline-flex items-center rounded-full px-2.5 py-0.5 font-mono text-[11px] uppercase tracking-wider w-fit ${
                      STATUS_PILL_CLASSES[user.status] ?? 'bg-surface-sunken text-ink-muted'
                    }`}
                  >
                    {user.status}
                  </span>
                  <span className="font-mono text-xs text-ink-subtle">
                    {formatRelativeTime(user.createdAt)}
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
          <span className="w-40 h-3.5 rounded bg-surface-sunken" />
          <span className="w-24 h-3.5 rounded bg-surface-sunken" />
          <span className="w-28 h-3.5 rounded bg-surface-sunken" />
          <span className="w-16 h-3.5 rounded bg-surface-sunken" />
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
