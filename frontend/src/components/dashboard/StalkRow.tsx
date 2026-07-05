import type { Stalk } from '../../lib/types';
import { StatusDot } from './StatusDot';
import { StatePill } from './StatePill';
import { SparklinePlaceholder } from './SparklinePlaceholder';

type StalkRowProps = {
  stalk: Stalk;
  onClick: () => void;
};

const GRID_COLS = 'grid-cols-[2fr_1fr_2fr_0.5fr_0.5fr_0.3fr]';

export function StalkRow({ stalk, onClick }: StalkRowProps) {
  // TEMPORARY: displays healthIndex as uptime proxy. Real uptime from
  // /api/stalks/{id}/pulses/uptime lands in Commit 4.
  const uptime = stalk.healthIndex;
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
          <p className="font-display text-[18px] text-ink truncate">{stalk.name}</p>
          <p className="font-mono text-[12px] text-ink-muted truncate">{stalk.url}</p>
        </div>
      </div>

      <div className="flex items-center gap-1.5">
        <StatePill variant="reliability" state={stalk.reliabilityState} />
        <StatePill variant="latency" state={stalk.latencyState} />
      </div>

      {/* TODO: real sparklines land in Commit 4 */}
      <SparklinePlaceholder />

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
