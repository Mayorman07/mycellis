import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ThemeToggle } from './ThemeToggle';
import { useSession } from '../../lib/hooks/useSession';
import type { PlanTier } from '../../lib/types';

type DashboardHeaderProps = {
  orgName: string;
  planTier: PlanTier;
  userInitials: string;
};

const NAV_TABS = ['Overview', 'Status pages', 'Settings'] as const;

const ACTIVE_TAB_CLASSES = 'rounded-md bg-surface-raised px-3 py-1.5 text-sm font-semibold text-ink';
const INACTIVE_TAB_CLASSES = 'rounded-md px-3 py-1.5 text-sm text-ink-subtle';

const ACTIVE_MOBILE_TAB_CLASSES =
  'flex items-center min-h-[44px] px-3 rounded-md text-sm bg-surface-raised font-semibold text-ink';
const INACTIVE_MOBILE_TAB_CLASSES = 'flex items-center min-h-[44px] px-3 rounded-md text-sm text-ink-subtle';

// "Status pages" intentionally has no active state — its href opens the
// public status page in a new tab (see statusPagesHref below), so the
// current tab's pathname never actually becomes that route.
function isTabActive(tab: (typeof NAV_TABS)[number], pathname: string): boolean {
  if (tab === 'Overview') return pathname === '/dashboard';
  if (tab === 'Settings') return pathname.startsWith('/settings');
  return false;
}

export function DashboardHeader({ orgName, planTier, userInitials }: DashboardHeaderProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const session = useSession();
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  // "See what customers see" — opens in a new tab rather than navigating the
  // dashboard away. undefined href until the session resolves; an <a> with
  // no href isn't clickable, which is the safe fallback for that window.
  const statusPagesHref = session.data
    ? `/status/${session.data.organization.slug}`
    : undefined;
  const isSuperAdmin = session.data?.user.roles.includes('SUPER_ADMIN') ?? false;
  const isSuperAdminActive = location.pathname.startsWith('/super-admin');

  function closeMenu() {
    setIsMenuOpen(false);
  }

  // ESC closes the drawer.
  useEffect(() => {
    if (!isMenuOpen) return;
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') closeMenu();
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isMenuOpen]);

  // Body scroll lock while the drawer is open.
  useEffect(() => {
    if (!isMenuOpen) return;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = '';
    };
  }, [isMenuOpen]);

  return (
    <header className="border-b border-hairline bg-surface px-6 py-3 flex items-center justify-between gap-6">
      <div className="group flex items-center gap-2 -mx-2 -my-1 rounded-md px-2 py-1 transition-colors duration-200 ease-out hover:bg-[color-mix(in_srgb,var(--color-surface-raised)_60%,transparent)]">
        <Link
          to="/dashboard"
          className="flex items-center gap-2 focus-visible:outline-none focus-visible:[box-shadow:0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]"
        >
          <div className="w-8 h-8 rounded-md bg-brand text-brand-fg flex items-center justify-center font-display text-sm transition-transform duration-200 ease-out group-hover:scale-[1.02]">
            {orgName.charAt(0).toUpperCase()}
          </div>
          <span className="font-display text-ink transition-colors duration-200 ease-out group-hover:underline group-hover:decoration-brand group-hover:decoration-1 group-hover:underline-offset-4">
            {orgName}
          </span>
        </Link>
        <span className="font-mono uppercase text-[10px] tracking-wider text-ink-muted bg-accent rounded-full px-2 py-0.5">
          {planTier}
        </span>
        <ChevronIcon className="text-ink-subtle" />
      </div>

      <nav className="hidden lg:flex items-center gap-1">
        {NAV_TABS.map((tab) => {
          const className = isTabActive(tab, location.pathname) ? ACTIVE_TAB_CLASSES : INACTIVE_TAB_CLASSES;

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
              onClick={() => navigate(tab === 'Overview' ? '/dashboard' : '/settings')}
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
            className={isSuperAdminActive ? ACTIVE_TAB_CLASSES : INACTIVE_TAB_CLASSES}
          >
            Super admin
          </button>
        )}
      </nav>

      <div className="hidden lg:flex items-center gap-3">
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

      <button
        type="button"
        onClick={() => setIsMenuOpen(true)}
        aria-label="Open menu"
        className="flex lg:hidden items-center justify-center h-11 w-11 -mr-2 rounded-md text-ink hover:bg-surface-raised focus-visible:outline-none focus-visible:[box-shadow:0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]"
      >
        <HamburgerIcon />
      </button>

      {/* Mobile nav drawer — always mounted below lg so the slide transition
          has something to animate from; lg:hidden removes it from desktop
          entirely regardless of isMenuOpen, so it can never open there. */}
      <div className="lg:hidden" aria-hidden={!isMenuOpen}>
        <div
          onClick={closeMenu}
          className={`fixed inset-0 z-40 bg-black/40 transition-opacity duration-300 ease-out ${
            isMenuOpen ? 'opacity-100' : 'pointer-events-none opacity-0'
          }`}
        />
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Navigation menu"
          className={`fixed inset-y-0 right-0 z-50 flex h-full w-4/5 max-w-sm flex-col bg-surface border-l border-hairline transition-transform duration-300 ease-out ${
            isMenuOpen ? 'translate-x-0' : 'translate-x-full pointer-events-none'
          }`}
        >
          <div className="flex items-center justify-between px-4 py-3 border-b border-hairline">
            <span className="font-mono uppercase text-xs tracking-wider text-ink-subtle">Menu</span>
            <button
              type="button"
              onClick={closeMenu}
              aria-label="Close menu"
              className="flex items-center justify-center h-11 w-11 -mr-2 rounded-md text-ink-subtle hover:text-ink hover:bg-surface-raised focus-visible:outline-none focus-visible:[box-shadow:0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]"
            >
              <CloseIcon />
            </button>
          </div>

          <nav className="flex flex-col p-2">
            {NAV_TABS.map((tab) => {
              const className = isTabActive(tab, location.pathname)
                ? ACTIVE_MOBILE_TAB_CLASSES
                : INACTIVE_MOBILE_TAB_CLASSES;

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
                        return;
                      }
                      closeMenu();
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
                  onClick={() => {
                    navigate(tab === 'Overview' ? '/dashboard' : '/settings');
                    closeMenu();
                  }}
                  className={className}
                >
                  {tab}
                </button>
              );
            })}
            {isSuperAdmin && (
              <button
                type="button"
                onClick={() => {
                  navigate('/super-admin/organizations');
                  closeMenu();
                }}
                className={isSuperAdminActive ? ACTIVE_MOBILE_TAB_CLASSES : INACTIVE_MOBILE_TAB_CLASSES}
              >
                Super admin
              </button>
            )}
            <button
              type="button"
              onClick={() => {
                navigate('/guide');
                closeMenu();
              }}
              className="flex items-center min-h-[44px] px-3 rounded-md text-sm text-ink-subtle"
            >
              2-minute guide
            </button>
          </nav>

          <div className="mt-auto p-4 border-t border-hairline">
            <div className="relative mb-4">
              <input
                type="text"
                placeholder="Search or jump to..."
                className="w-full rounded-md border border-hairline bg-surface-raised pl-3 pr-12 py-2.5 text-sm text-ink placeholder:text-ink-subtle focus:outline-none"
              />
              <span className="absolute right-2 top-1/2 -translate-y-1/2 rounded border border-hairline px-1.5 py-0.5 font-mono text-[10px] text-ink-subtle">
                ⌘K
              </span>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-brand text-brand-fg flex items-center justify-center text-sm font-medium">
                  {userInitials}
                </div>
                <span className="text-sm text-ink">Account</span>
              </div>
              <ThemeToggle />
            </div>
          </div>
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

function HamburgerIcon() {
  return (
    <svg viewBox="0 0 20 20" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M3 5.5h14M3 10h14M3 14.5h14" strokeLinecap="round" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M5 5l10 10M15 5L5 15" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
