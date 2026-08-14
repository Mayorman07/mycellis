import type { Pulse, ReliabilityState } from '../../lib/types';

type SparklineProps = {
  pulses: Pulse[];
  /** Parent stalk's reliabilityState — while AWAKENING, every bar renders
   * in the awakening color regardless of individual pulse outcomes, so the
   * sparkline doesn't contradict the state pill shown next to it. */
  reliabilityState: ReliabilityState;
  barWidth?: number;
  gap?: number;
  height?: number;
  slotCount?: number;
};

const DEFAULT_SLOT_COUNT = 40;
const MIN_BAR_HEIGHT = 4;

const AWAKENING_FILL = 'var(--color-state-awakening)';

const FILL_BY_STATE: Record<Pulse['reliabilityState'], string> = {
  // Pulse-level reliabilityState is derived per-pulse by the backend's
  // PulseMapper, which only ever emits HEALTHY/DEGRADED/DOWN — AWAKENING and
  // DORMANT are stalk-aggregate-only and can't appear on a real Pulse. Both
  // are still required here because Pulse shares the same ReliabilityState
  // type as Stalk; these two entries are unreachable in practice.
  AWAKENING: AWAKENING_FILL,
  HEALTHY: 'var(--color-state-healthy)',
  DEGRADED: 'var(--color-state-stressed)',
  DOWN: 'var(--color-state-down)',
  DORMANT: 'var(--color-state-dormant)',
};

export function Sparkline({
  pulses,
  reliabilityState,
  barWidth = 3,
  gap = 1,
  height = 24,
  slotCount = DEFAULT_SLOT_COUNT,
}: SparklineProps) {
  const isAwakening = reliabilityState === 'AWAKENING';
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
            fill={isAwakening ? AWAKENING_FILL : FILL_BY_STATE[pulse.reliabilityState]}
          />
        );
      })}
    </svg>
  );
}
