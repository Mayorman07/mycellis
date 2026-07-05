import type { ReliabilityState, LatencyState } from '../../lib/types';

type StatePillProps =
  | { variant: 'reliability'; state: ReliabilityState }
  | { variant: 'latency'; state: LatencyState };

const COLOR_VAR: Record<ReliabilityState, string> = {
  HEALTHY: '--color-state-healthy',
  DEGRADED: '--color-state-stressed',
  DOWN: '--color-state-down',
  DORMANT: '--color-state-dormant',
};

const TEXT_CLASS: Record<ReliabilityState, string> = {
  HEALTHY: 'text-state-healthy',
  DEGRADED: 'text-state-stressed',
  DOWN: 'text-state-down',
  DORMANT: 'text-state-dormant',
};

const DOT_CLASS: Record<ReliabilityState, string> = {
  HEALTHY: 'bg-state-healthy',
  DEGRADED: 'bg-state-stressed',
  DOWN: 'bg-state-down',
  DORMANT: 'bg-state-dormant',
};

export function StatePill(props: StatePillProps) {
  // Latency pill only ever appears for STRESSED; NORMAL means no pill at all.
  if (props.variant === 'latency' && props.state === 'NORMAL') {
    return null;
  }

  // Latency's STRESSED pill is styled identically to a DEGRADED reliability pill.
  const key: ReliabilityState = props.variant === 'latency' ? 'DEGRADED' : props.state;

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 font-mono text-[10px] uppercase tracking-wider ${TEXT_CLASS[key]}`}
      // Tailwind's opacity modifiers can't decompose our CSS-variable-based
      // colors into channels, so the 15%-alpha tint is mixed manually here.
      style={{ backgroundColor: `color-mix(in srgb, var(${COLOR_VAR[key]}) 15%, transparent)` }}
    >
      <span className={`w-1 h-1 rounded-full ${DOT_CLASS[key]}`} />
      {props.state}
    </span>
  );
}
