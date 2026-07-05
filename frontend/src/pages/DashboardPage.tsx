import { useTheme } from '../lib/theme';
import { useSession } from '../lib/hooks/useSession';

export default function DashboardPage() {
  const [, setTheme] = useTheme();
  const { data } = useSession();

  return (
    <div className="p-8 text-ink">
      Dashboard coming soon.
      {/* TEMPORARY: proof-of-life display for Commit 1 verification. */}
      {/* Removed in Commit 3 when the real DashboardHeader lands. */}
      {data && (
        <p className="mt-4 text-ink-muted">
          Signed in as {data.user.firstName} {data.user.lastName} — {data.organization.name} (
          {data.organization.planTier})
        </p>
      )}
      {/* TEMPORARY: theme toggle for Commit 0 verification. */}
      {/* Removed in Commit 3 when the real DashboardHeader lands. */}
      <div className="mt-4 flex gap-2">
        <button
          type="button"
          onClick={() => setTheme('cream')}
          className="rounded-md border border-hairline bg-surface-raised px-3 py-1.5 text-sm text-ink"
        >
          Cream
        </button>
        <button
          type="button"
          onClick={() => setTheme('white')}
          className="rounded-md border border-hairline bg-surface-raised px-3 py-1.5 text-sm text-ink"
        >
          White
        </button>
        <button
          type="button"
          onClick={() => setTheme('black')}
          className="rounded-md border border-hairline bg-surface-raised px-3 py-1.5 text-sm text-ink"
        >
          Black
        </button>
      </div>
    </div>
  );
}