import { useTheme } from '../lib/theme';

export default function DashboardPage() {
  const [, setTheme] = useTheme();

  return (
    <div className="p-8 text-ink">
      Dashboard coming soon.
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