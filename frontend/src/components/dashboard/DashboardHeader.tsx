import { Link, useNavigate } from 'react-router-dom';
import { ThemeToggle } from './ThemeToggle';
import { useSession } from '../../lib/hooks/useSession';
import type { PlanTier } from '../../lib/types';

type DashboardHeaderProps = {
  orgName: string;
  planTier: PlanTier;
  userInitials: string;
};

const NAV_TABS = ['Overview', 'Status pages', 'Settings'] as const;

export function DashboardHeader({ orgName, planTier, userInitials }: DashboardHeaderProps) {
  const navigate = useNavigate();
  const session = useSession();
  // "See what customers see" — opens in a new tab rather than navigating the
  // dashboard away. undefined href until the session resolves; an <a> with
  // no href isn't clickable, which is the safe fallback for that window.
  const statusPagesHref = session.data
    ? `/status/${session.data.organization.slug}`
    : undefined;
  const isSuperAdmin = session.data?.user.roles.includes('SUPER_ADMIN') ?? false;

  return (
    <header className="border-b border-hairline bg-surface px-6 py-3 flex items-center justify-between gap-6">
      <Link
        to="/dashboard"
        className="group flex items-center gap-2 -mx-2 -my-1 rounded-md px-2 py-1 transition-colors duration-200 ease-out hover:bg-[color-mix(in_srgb,var(--color-surface-raised)_60%,transparent)] focus-visible:outline-none focus-visible:[box-shadow:0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]"
      >
        <div className="w-8 h-8 rounded-md bg-brand text-brand-fg flex items-center justify-center font-display text-sm transition-transform duration-200 ease-out group-hover:scale-[1.02]">
          {orgName.charAt(0).toUpperCase()}
        </div>
        <span className="font-display text-ink transition-colors duration-200 ease-out group-hover:underline group-hover:decoration-brand group-hover:decoration-1 group-hover:underline-offset-4">
          {orgName}
        </span>
        <span className="font-mono uppercase text-[10px] tracking-wider text-ink-muted bg-accent rounded-full px-2 py-0.5">
          {planTier}
        </span>
        <ChevronIcon className="text-ink-subtle" />
      </Link>

      <nav className="flex items-center gap-1">
        {NAV_TABS.map((tab) => {
          const className =
            tab === 'Overview'
              ? 'rounded-md bg-surface-raised px-3 py-1.5 text-sm font-semibold text-ink'
              : 'rounded-md px-3 py-1.5 text-sm text-ink-subtle';

          if (tab === 'Status pages') {
            return (
              <a
                key={tab}
                href={statusPagesHref}
                target="_blank"
                rel="noopener noreferrer"
                aria-disabled={!statusPagesHref}
                onClick={(event) => {
                  if (!statusPagesHref) {
                    event.preventDefault();
                  }
                }}
                className={className}
              >
                {tab}
              </a>
            );
          }

          return (
            <button
              key={tab}
              type="button"
              onClick={tab === 'Settings' ? () => navigate('/settings') : undefined}
              className={className}
            >
              {tab}
            </button>
          );
        })}
        {isSuperAdmin && (
          <button
            type="button"
            onClick={() => navigate('/super-admin/organizations')}
            className="rounded-md px-3 py-1.5 text-sm text-ink-subtle"
          >
            Super admin
          </button>
        )}
      </nav>

      <div className="flex items-center gap-3">
        <div className="relative">
          <input
            type="text"
            placeholder="Search or jump to..."
            className="rounded-md border border-hairline bg-surface-raised pl-3 pr-12 py-1.5 text-sm text-ink placeholder:text-ink-subtle focus:outline-none"
          />
          <span className="absolute right-2 top-1/2 -translate-y-1/2 rounded border border-hairline px-1.5 py-0.5 font-mono text-[10px] text-ink-subtle">
            ⌘K
          </span>
        </div>
        <ThemeToggle />
        <div className="w-8 h-8 rounded-full bg-brand text-brand-fg flex items-center justify-center text-sm font-medium">
          {userInitials}
        </div>
      </div>
    </header>
  );
}

function ChevronIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 12 12" width="10" height="10" fill="none" stroke="currentColor" strokeWidth="1.5" className={className}>
      <path d="M3 4.5 6 7.5 9 4.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
