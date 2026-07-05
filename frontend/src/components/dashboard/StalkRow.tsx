import type { Pulse, Stalk } from '../../lib/types';
import { StatusDot } from './StatusDot';
import { StatePill } from './StatePill';
import { Sparkline } from './Sparkline';

type StalkRowProps = {
  stalk: Stalk;
  pulses: Pulse[];
  onClick: () => void;
};

const GRID_COLS = 'grid-cols-[2fr_1fr_2fr_0.5fr_0.5fr_0.3fr]';

export function StalkRow({ stalk, pulses, onClick }: StalkRowProps) {
  // Uptime now derives from real pulses (Commit 3 used healthIndex as a placeholder proxy).
  // Brand-new stalks with no pulse history yet still fall back to healthIndex.
  const uptime =
    pulses.length === 0
      ? stalk.healthIndex
      : (pulses.filter((p) => p.reliabilityState === 'HEALTHY').length / pulses.length) * 100;
  const uptimeColorClass =
    uptime >= 99 ? 'text-ink' : uptime >= 95 ? 'text-state-stressed' : 'text-state-down';

  return (
    <div
      onClick={onClick}
      className={`grid ${GRID_COLS} items-center gap-4 py-3 px-4 border-b border-hairline hover:bg-surface-raised cursor-pointer transition-colors`}
    >
      <div className="flex items-center gap-3 min-w-0">
        <StatusDot reliabilityState={stalk.reliabilityState} />
        <div className="min-w-0">
          <p className="font-display text-[18px] text-ink truncate">{stalk.nickname}</p>
          <p className="font-mono text-[12px] text-ink-muted truncate">{stalk.url}</p>
        </div>
      </div>

      <div className="flex items-center gap-1.5">
        <StatePill variant="reliability" state={stalk.reliabilityState} />
        <StatePill variant="latency" state={stalk.latencyState} />
      </div>

      <Sparkline pulses={pulses} />

      <div className="font-mono text-sm text-ink">
        {stalk.averageLatencyMs === null ? (
          '—'
        ) : (
          <>
            {stalk.averageLatencyMs}
            <span className="text-ink-muted text-xs"> ms</span>
          </>
        )}
      </div>

      <div className={`font-display text-[15px] ${uptimeColorClass}`}>{uptime.toFixed(1)}%</div>

      <ChevronRightIcon />
    </div>
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
      className="text-ink-subtle justify-self-end"
    >
      <path d="M6 3.5 10.5 8 6 12.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
