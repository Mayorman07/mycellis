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

  // Shared between both renderings below — same value, two different layouts.
  const latencyLabel =
    stalk.averageLatencyMs === null ? (
      '—'
    ) : (
      <>
        {stalk.averageLatencyMs}
        <span className="text-ink-muted text-xs"> ms</span>
      </>
    );

  return (
    <>
      {/* Desktop (md+): unchanged 6-column grid row. */}
      <div
        onClick={onClick}
        className={`hidden md:grid ${GRID_COLS} items-center gap-4 py-3 px-4 border-b border-hairline hover:bg-surface-raised cursor-pointer transition-colors`}
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

        <Sparkline pulses={pulses} reliabilityState={stalk.reliabilityState} />

        <div className="font-mono text-sm text-ink">{latencyLabel}</div>

        <div className={`font-display text-[15px] ${uptimeColorClass}`}>{uptime.toFixed(1)}%</div>

        <ChevronRightIcon />
      </div>

      {/* Mobile (below md): stacked card. Sparkline.tsx renders a fixed-size
          SVG (width/height attrs from its own defaults) with no className
          prop to hook into — [&>svg]:w-full/h-auto overrides those via CSS
          from here instead of touching that file.
          role="button" + tabIndex + onKeyDown make the whole card keyboard-
          operable, not just click-operable — intentionally NOT applied to
          the desktop row above, which keeps its existing click-only pattern
          unchanged, per the "mobile card only" scope of this pass. */}
      <div
        onClick={onClick}
        onKeyDown={(event) => {
          if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            onClick();
          }
        }}
        role="button"
        tabIndex={0}
        className="md:hidden min-h-[44px] rounded-md border border-hairline bg-surface-raised p-4 cursor-pointer transition-colors hover:bg-surface-sunken focus-visible:outline-none focus-visible:[box-shadow:0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]"
      >
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2 min-w-0">
            <StatusDot reliabilityState={stalk.reliabilityState} />
            <p className="font-semibold text-base text-ink truncate">{stalk.nickname}</p>
          </div>
          <div className="flex items-center gap-1.5 flex-shrink-0">
            <StatePill variant="reliability" state={stalk.reliabilityState} />
            <StatePill variant="latency" state={stalk.latencyState} />
          </div>
        </div>

        <p className="text-sm text-ink-subtle truncate mt-1">{stalk.url}</p>

        <div className="w-full mt-3 [&>svg]:w-full [&>svg]:h-auto">
          <Sparkline pulses={pulses} reliabilityState={stalk.reliabilityState} />
        </div>

        <div className="flex items-center gap-6 mt-3">
          <div>
            <p className="font-mono text-[10px] uppercase tracking-wider text-ink-subtle">Latency</p>
            <p className="font-mono text-sm text-ink mt-0.5">{latencyLabel}</p>
          </div>
          <div>
            <p className="font-mono text-[10px] uppercase tracking-wider text-ink-subtle">Uptime</p>
            <p className={`font-display text-[15px] mt-0.5 ${uptimeColorClass}`}>{uptime.toFixed(1)}%</p>
          </div>
        </div>
      </div>
    </>
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
