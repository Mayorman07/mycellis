import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

type DashboardHeroProps = {
  stalksCount: number;
  lastSyncedAt: number;
};

function formatSynced(elapsedMs: number): string {
  const seconds = Math.max(0, Math.floor(elapsedMs / 1000));
  if (seconds < 60) return `${seconds}S AGO`;
  return `${Math.floor(seconds / 60)}M AGO`;
}

export function DashboardHero({ stalksCount, lastSyncedAt }: DashboardHeroProps) {
  const navigate = useNavigate();

  // Render the parent with key={lastSyncedAt} so a resync remounts this
  // component and resets the ticker, instead of resetting state in an effect.
  const [elapsedMs, setElapsedMs] = useState(() => Date.now() - lastSyncedAt);

  useEffect(() => {
    const id = setInterval(() => setElapsedMs(Date.now() - lastSyncedAt), 1000);
    return () => clearInterval(id);
  }, [lastSyncedAt]);

  return (
    <div className="flex justify-between items-baseline gap-6 py-10">
      <div>
        <p className="font-mono uppercase text-xs tracking-wider text-ink-muted">
          MYCELLIS · LIVE · SYNCED {formatSynced(elapsedMs)}
        </p>
        <h1 className="font-display font-[450] text-[64px] leading-tight tracking-[-0.02em] text-ink mt-2">
          {stalksCount} stalks, breathing.
        </h1>
      </div>
      <button
        type="button"
        onClick={() => navigate('/stalks/new')}
        className="rounded-md border border-brand bg-brand px-4 py-2 text-sm font-medium text-brand-fg whitespace-nowrap"
      >
        + New stalk
      </button>
    </div>
  );
}
