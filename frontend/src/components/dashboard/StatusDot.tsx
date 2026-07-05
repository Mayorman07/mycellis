import type { ReliabilityState } from '../../lib/types';

type StatusDotProps = {
  reliabilityState: ReliabilityState;
};

const DOT_COLOR_CLASS: Record<ReliabilityState, string> = {
  HEALTHY: 'text-state-healthy',
  DEGRADED: 'text-state-stressed',
  DOWN: 'text-state-down',
  DORMANT: 'text-state-dormant',
};

export function StatusDot({ reliabilityState }: StatusDotProps) {
  const isDormant = reliabilityState === 'DORMANT';

  return (
    <span
      className={`group relative inline-flex w-2.5 h-2.5 hover:scale-125 transition-transform ${DOT_COLOR_CLASS[reliabilityState]}`}
    >
      {/* halo: fades in on hover, sized larger than the dot itself */}
      <span className="absolute -inset-[5px] rounded-full bg-current opacity-0 group-hover:opacity-20 transition-opacity" />
      <span
        className="relative w-2.5 h-2.5 rounded-full bg-current group-hover:[animation-play-state:paused]"
        style={isDormant ? undefined : { animation: 'mycellis-breath 2.6s ease-in-out infinite' }}
      />
    </span>
  );
}
