import { ThemeToggle } from './ThemeToggle';
import type { PlanTier } from '../../lib/types';

type DashboardHeaderProps = {
  orgName: string;
  planTier: PlanTier;
  userInitials: string;
};

const NAV_TABS = ['Overview', 'Status pages', 'Settings'] as const;

export function DashboardHeader({ orgName, planTier, userInitials }: DashboardHeaderProps) {
  return (
    <header className="border-b border-hairline bg-surface px-6 py-3 flex items-center justify-between gap-6">
      <div className="flex items-center gap-2 cursor-default">
        <div className="w-8 h-8 rounded-md bg-brand text-brand-fg flex items-center justify-center font-display text-sm">
          {orgName.charAt(0).toUpperCase()}
        </div>
        <span className="font-display text-ink">{orgName}</span>
        <span className="font-mono uppercase text-[10px] tracking-wider text-ink-muted bg-accent rounded-full px-2 py-0.5">
          {planTier}
        </span>
        <ChevronIcon className="text-ink-subtle" />
      </div>

      {/* TODO: wire nav routing when Status pages and Settings pages land */}
      <nav className="flex items-center gap-1">
        {NAV_TABS.map((tab) => (
          <button
            key={tab}
            type="button"
            className={
              tab === 'Overview'
                ? 'rounded-md bg-surface-raised px-3 py-1.5 text-sm font-semibold text-ink'
                : 'rounded-md px-3 py-1.5 text-sm text-ink-subtle'
            }
          >
            {tab}
          </button>
        ))}
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
