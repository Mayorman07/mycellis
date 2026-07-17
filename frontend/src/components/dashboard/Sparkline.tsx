import type { Pulse } from '../../lib/types';

type SparklineProps = {
  pulses: Pulse[];
  barWidth?: number;
  gap?: number;
  height?: number;
  slotCount?: number;
};

const DEFAULT_SLOT_COUNT = 40;
const MIN_BAR_HEIGHT = 4;

const FILL_BY_STATE: Record<Pulse['reliabilityState'], string> = {
  HEALTHY: 'var(--color-state-healthy)',
  DEGRADED: 'var(--color-state-stressed)',
  DOWN: 'var(--color-state-down)',
  DORMANT: 'var(--color-state-dormant)',
};

export function Sparkline({
  pulses,
  barWidth = 3,
  gap = 1,
  height = 24,
  slotCount = DEFAULT_SLOT_COUNT,
}: SparklineProps) {
  const width = slotCount * barWidth + (slotCount - 1) * gap;

  // Backend returns newest-first; render oldest-to-newest so the newest pulse lands rightmost.
  const ordered = [...pulses].sort(
    (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
  );
  const maxLatency = Math.max(1, ...ordered.map((p) => p.latencyMs ?? 0));

  // Fewer pulses than slots: right-align by padding empty slots on the left.
  const startSlot = slotCount - ordered.length;

  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`}>
      {ordered.map((pulse, i) => {
        const slot = startSlot + i;
        const barHeight =
          pulse.latencyMs == null
            ? MIN_BAR_HEIGHT
            : MIN_BAR_HEIGHT + (pulse.latencyMs / maxLatency) * (height - MIN_BAR_HEIGHT);

        return (
          <rect
            key={pulse.id}
            x={slot * (barWidth + gap)}
            y={height - barHeight}
            width={barWidth}
            height={barHeight}
            fill={FILL_BY_STATE[pulse.reliabilityState]}
          />
        );
      })}
    </svg>
  );
}
