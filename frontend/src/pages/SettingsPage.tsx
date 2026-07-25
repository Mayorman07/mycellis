import { useState, type ReactNode } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useSession } from '../lib/hooks/useSession';
import { logout } from '../lib/api/auth';
import { updateAlertPreferences } from '../lib/api/me';
import type { ApiError } from '../lib/api/client';
import type { MeResponse, PlanTier, User } from '../lib/types';

type LocationState = { flash?: string } | null;

// Backend's PlanTier enum only has these three values today (FREE/PRO/
// ENTERPRISE) — no STARTER/BUSINESS tier exists on either side.
const PLAN_PILL_CLASSES: Record<PlanTier, string> = {
  FREE: 'bg-surface-sunken text-ink-muted',
  PRO: 'bg-[color-mix(in_srgb,var(--color-brand)_25%,transparent)] text-ink',
  ENTERPRISE: 'bg-brand text-brand-fg',
};

const ROLE_LABELS: Record<string, string> = {
  SUPER_ADMIN: 'Super Admin',
  OWNER: 'Owner',
  ADMIN: 'Admin',
  MEMBER: 'Member',
};

function formatRole(role: string | undefined): string {
  if (!role) {
    return '—';
  }
  return (
    ROLE_LABELS[role] ??
    role
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ')
  );
}

function formatMemberSince(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
}

function formatMemberCount(count: number): string {
  return `${count} member${count === 1 ? '' : 's'}`;
}

export default function SettingsPage() {
  const session = useSession();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const location = useLocation();

  const state = location.state as LocationState;
  const [flash, setFlash] = useState<string | null>(state?.flash ?? null);

  const logoutMutation = useMutation<Awaited<ReturnType<typeof logout>>, ApiError, void>({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.clear();
      navigate('/login', { replace: true });
    },
    onError: () => {
      // Backend session might already be dead — clear anyway.
      queryClient.clear();
      navigate('/login', { replace: true });
    },
  });

  if (session.isLoading) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center">
        <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">Loading…</p>
      </div>
    );
  }

  if (session.isError || !session.data) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center">
        <p className="text-sm text-ink-muted">Couldn't load your account. Refresh to try again.</p>
      </div>
    );
  }

  const { user, organization } = session.data;

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-[640px] mx-auto px-6 py-12">
        <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-4">
          MYCELLIS · SETTINGS
        </p>
        <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-3">
          Settings.
        </h1>
        <p className="text-ink-muted mb-10">Your workspace and account.</p>

        {flash && (
          <div
            role="status"
            className="mb-8 flex items-center justify-between gap-3 rounded-md border border-hairline bg-[color-mix(in_srgb,var(--color-state-healthy)_15%,transparent)] px-4 py-3"
          >
            <p className="text-sm text-ink">{flash}</p>
            <button
              type="button"
              onClick={() => setFlash(null)}
              aria-label="Dismiss"
              className="flex-shrink-0 text-ink-muted hover:text-ink"
            >
              <XIcon />
            </button>
          </div>
        )}

        <section className="mb-8">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            Workspace
          </p>
          <div className="rounded-lg border border-hairline bg-surface-raised py-2 px-6">
            <InfoRow label="Name" value={organization.name} />
            <InfoRow label="Slug" value={<span className="font-mono">{organization.slug}</span>} />
            <InfoRow
              label="Plan"
              value={
                <span
                  className={`inline-flex items-center rounded-full px-2.5 py-0.5 font-mono text-[11px] uppercase tracking-wider ${PLAN_PILL_CLASSES[organization.planTier]}`}
                >
                  {organization.planTier}
                </span>
              }
            />
            <InfoRow
              label="Members"
              value={<span className="font-mono">{formatMemberCount(organization.memberCount)}</span>}
            />
          </div>
        </section>

        <section className="mb-8">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            Account
          </p>
          <div className="rounded-lg border border-hairline bg-surface-raised py-2 px-6">
            <InfoRow label="Name" value={`${user.firstName} ${user.lastName}`} />
            <InfoRow label="Email" value={user.email} />
            <InfoRow label="Role" value={formatRole(user.roles[0])} />
            <InfoRow label="Member since" value={formatMemberSince(user.createdAt)} />
          </div>
        </section>

        <AlertsSection user={user} />

        <section>
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            Actions
          </p>
          <div className="rounded-lg border border-hairline bg-surface-raised overflow-hidden">
            <ActionRow label="Change password" onClick={() => navigate('/settings/change-password')} />
            <ActionRow label="Change email" onClick={() => navigate('/settings/change-email')} />
            <ActionRow
              label="Log out"
              busyLabel="Logging out…"
              isBusy={logoutMutation.isPending}
              disabled={logoutMutation.isPending}
              onClick={() => logoutMutation.mutate()}
            />
          </div>
        </section>
      </main>
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-center justify-between py-4 border-b border-hairline last:border-b-0">
      <span className="font-mono uppercase text-xs tracking-wider text-ink-subtle">{label}</span>
      <span className="text-sm text-ink text-right ml-4">{value}</span>
    </div>
  );
}

function ActionRow({
  label,
  onClick,
  disabled,
  busyLabel,
  isBusy,
}: {
  label: string;
  onClick: () => void;
  disabled?: boolean;
  busyLabel?: string;
  isBusy?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-busy={isBusy}
      className="w-full flex items-center justify-between py-4 px-6 border-b border-hairline last:border-b-0 text-left hover:bg-surface-sunken disabled:cursor-default disabled:opacity-60"
    >
      <span className="text-sm text-ink">{isBusy && busyLabel ? busyLabel : label}</span>
      <ChevronRightIcon />
    </button>
  );
}

function ChevronRightIcon() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      className="text-ink-subtle flex-shrink-0"
    >
      <path d="M6 3.5 10.5 8 6 12.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

// Mounted only once `user` is real, loaded data (parent guards on session
// loading/error before rendering this) — so local state can initialize
// directly from props on first render, no hydration effect needed.
function AlertsSection({ user }: { user: User }) {
  const queryClient = useQueryClient();
  const [alertEmailInput, setAlertEmailInput] = useState(user.alertEmail ?? user.email);
  const [alertsEnabled, setAlertsEnabled] = useState(user.alertsEnabled);

  const alertPrefsMutation = useMutation<MeResponse, ApiError, { alertEmail: string; alertsEnabled: boolean }>({
    mutationFn: ({ alertEmail, alertsEnabled: enabled }) =>
      updateAlertPreferences({
        alertEmail: alertEmail.trim() === '' ? null : alertEmail.trim(),
        alertsEnabled: enabled,
      }),
    onSuccess: (data) => {
      queryClient.setQueryData(['me'], data);
      setAlertEmailInput(data.user.alertEmail ?? data.user.email);
      setAlertsEnabled(data.user.alertsEnabled);
    },
  });

  return (
    <section className="mb-8">
      <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">Alerts</p>
      <div className="rounded-lg border border-hairline bg-surface-raised py-2 px-6">
        <div className="flex items-center justify-between py-4 border-b border-hairline">
          <span className="font-mono uppercase text-xs tracking-wider text-ink-subtle flex-shrink-0">
            Alert email
          </span>
          <input
            type="email"
            value={alertEmailInput}
            onChange={(event) => setAlertEmailInput(event.target.value)}
            onBlur={() => {
              const currentlySaved = user.alertEmail ?? user.email;
              if (alertEmailInput.trim() !== currentlySaved) {
                alertPrefsMutation.mutate({ alertEmail: alertEmailInput, alertsEnabled });
              }
            }}
            placeholder={user.email}
            className="text-sm text-ink text-right ml-4 bg-transparent focus:outline-none placeholder:text-ink-subtle"
          />
        </div>
        <div className="flex items-center justify-between py-4">
          <span className="text-sm text-ink">Send me alerts when my stalks go down</span>
          <ToggleSwitch
            checked={alertsEnabled}
            onChange={(next) => {
              setAlertsEnabled(next);
              alertPrefsMutation.mutate({ alertEmail: alertEmailInput, alertsEnabled: next });
            }}
          />
        </div>
      </div>
    </section>
  );
}

function ToggleSwitch({ checked, onChange }: { checked: boolean; onChange: (next: boolean) => void }) {
  return (
    <label className="inline-flex items-center cursor-pointer flex-shrink-0">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="peer sr-only"
      />
      <span
        aria-hidden="true"
        className={`relative w-10 h-6 rounded-full transition-colors duration-150 ease-in-out peer-focus-visible:[box-shadow:0_0_0_3px_color-mix(in_srgb,var(--color-brand)_15%,transparent)] ${
          checked ? 'bg-brand' : 'bg-surface-sunken border border-hairline'
        }`}
      >
        <span
          className={`absolute top-0.5 left-0.5 w-5 h-5 rounded-full bg-surface shadow transition-transform duration-150 ease-in-out ${
            checked ? 'translate-x-4' : 'translate-x-0'
          }`}
        />
      </span>
    </label>
  );
}

function XIcon() {
  return (
    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path d="M6 6l12 12M18 6L6 18" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
