import type { Pulse, Stalk } from '../../lib/types';
import { StalkRow } from './StalkRow';

type StalkTableProps = {
  stalks: Stalk[];
  pulsesByStalkId: Record<string, Pulse[]>;
};

const GRID_COLS = 'grid-cols-[2fr_1fr_2fr_0.5fr_0.5fr_0.3fr]';
const HEADER_LABELS = ['Stalk', 'State', 'Last 40 pulses', 'Latency', 'Uptime'];

export function StalkTable({ stalks, pulsesByStalkId }: StalkTableProps) {
  return (
    <div className="border border-hairline bg-surface-raised rounded-md overflow-hidden">
      <div className={`grid ${GRID_COLS} gap-4 py-3 px-4 border-b border-hairline`}>
        {/* No column for the chevron — the 6th grid track is left empty to match row layout. */}
        {HEADER_LABELS.map((label) => (
          <span key={label} className="font-mono text-xs uppercase tracking-wider text-ink-subtle">
            {label}
          </span>
        ))}
      </div>
      {stalks.map((stalk) => (
        <StalkRow
          key={stalk.id}
          stalk={stalk}
          pulses={pulsesByStalkId[stalk.id] ?? []}
          onClick={() => console.log(`Stalk clicked: ${stalk.nickname} - detail page coming in a later commit`)}
        />
      ))}
    </div>
  );
}
