import type { CSSProperties } from 'react';
import type { ReliabilityState } from '../../lib/types';

type StatusDotProps = {
  reliabilityState: ReliabilityState;
  /** Diameter in px. Defaults to the original 10px (w-2.5/h-2.5) dot size. */
  size?: number;
};

const DEFAULT_SIZE_PX = 10;

const DOT_COLOR_CLASS: Record<ReliabilityState, string> = {
  AWAKENING: 'text-state-awakening',
  HEALTHY: 'text-state-healthy',
  DEGRADED: 'text-state-stressed',
  DOWN: 'text-state-down',
  DORMANT: 'text-state-dormant',
};

export function StatusDot({ reliabilityState, size = DEFAULT_SIZE_PX }: StatusDotProps) {
  const isDormant = reliabilityState === 'DORMANT';
  // Halo keeps the same proportion (half the dot's own size) as the original
  // hardcoded -inset-[5px] on the default 10px dot.
  const haloInset = -(size / 2);

  return (
    <span
      className={`group relative inline-flex hover:scale-125 transition-transform ${DOT_COLOR_CLASS[reliabilityState]}`}
      style={{ width: size, height: size }}
    >
      {/* halo: fades in on hover, sized larger than the dot itself */}
      <span
        className="absolute rounded-full bg-current opacity-0 group-hover:opacity-20 transition-opacity"
        style={{ inset: haloInset }}
      />
      <span
        className="relative rounded-full bg-current group-hover:[animation-play-state:paused]"
        style={
          {
            width: size,
            height: size,
            ...(isDormant ? {} : { animation: 'mycellis-breath 2.6s ease-in-out infinite' }),
          } as CSSProperties
        }
      />
    </span>
  );
}
