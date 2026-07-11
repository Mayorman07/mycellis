import { useEffect, useState, type CSSProperties } from 'react';

type NetworkNode = {
  x: number;
  y: number;
  r: number;
  opacity: number;
  duration: number;
  delay: number;
};

type NetworkEdge = { a: number; b: number };

type NetworkPulse = { id: number; path: number[] };

// Hand-placed, not randomized — spans the full right column (viewBox 0-400)
// plus a little past the right edge so those nodes clip at the viewport
// boundary (suggests the network continues beyond what's visible). The two
// leftmost nodes carry low base opacity so the boundary with the form
// column reads as a gradient of presence rather than a hard edge.
const NETWORK_NODES: NetworkNode[] = [
  { x: 12, y: 380, r: 4, opacity: 0.2, duration: 3.4, delay: 0.2 },
  { x: 35, y: 630, r: 5, opacity: 0.25, duration: 2.8, delay: 0.9 },
  { x: 75, y: 190, r: 6, opacity: 0.35, duration: 3.9, delay: 0.4 },
  { x: 110, y: 720, r: 7, opacity: 0.5, duration: 2.6, delay: 1.3 },
  { x: 150, y: 460, r: 8, opacity: 0.55, duration: 3.1, delay: 0.0 },
  { x: 185, y: 630, r: 6, opacity: 0.5, duration: 4.0, delay: 0.7 },
  { x: 215, y: 260, r: 7, opacity: 0.6, duration: 2.9, delay: 1.1 },
  { x: 250, y: 510, r: 9, opacity: 0.7, duration: 3.6, delay: 0.3 },
  { x: 275, y: 700, r: 6, opacity: 0.55, duration: 2.5, delay: 1.4 },
  { x: 305, y: 360, r: 8, opacity: 0.75, duration: 3.3, delay: 0.6 },
  { x: 335, y: 560, r: 10, opacity: 0.85, duration: 4.2, delay: 0.1 },
  { x: 365, y: 160, r: 7, opacity: 0.6, duration: 2.7, delay: 1.0 },
  { x: 390, y: 440, r: 9, opacity: 0.8, duration: 3.7, delay: 0.5 },
  { x: 403, y: 640, r: 8, opacity: 0.9, duration: 3.0, delay: 1.5 },
];

// Mesh topology derives from node geometry (nearest neighbors), not a fixed
// list — keeps the graph organic without hand-reasoning about which pairs
// are closest.
function buildNearestNeighborEdges(nodes: NetworkNode[], neighborsPerNode: number): NetworkEdge[] {
  const seen = new Set<string>();
  const edges: NetworkEdge[] = [];

  nodes.forEach((node, index) => {
    const nearest = nodes
      .map((other, otherIndex) => ({
        index: otherIndex,
        distance: Math.hypot(other.x - node.x, other.y - node.y),
      }))
      .filter((entry) => entry.index !== index)
      .sort((left, right) => left.distance - right.distance)
      .slice(0, neighborsPerNode);

    nearest.forEach((entry) => {
      const a = Math.min(index, entry.index);
      const b = Math.max(index, entry.index);
      const key = `${a}-${b}`;
      if (!seen.has(key)) {
        seen.add(key);
        edges.push({ a, b });
      }
    });
  });

  return edges;
}

const NETWORK_EDGES = buildNearestNeighborEdges(NETWORK_NODES, 3);

// "Birth of the network" timing: nodes fade in staggered, then lines draw
// in once nodes have appeared, then ambient breathing + pulses begin.
const NODE_FADE_IN_DURATION_S = 0.5;
const NODE_FADE_IN_SPREAD_S = 1.0;
const LINE_DRAW_IN_DELAY_S = NODE_FADE_IN_SPREAD_S + NODE_FADE_IN_DURATION_S;
const LINE_DRAW_IN_DURATION_S = 0.8;
const LINE_DRAW_IN_STAGGER_S = 0.2;
const BIRTH_COMPLETE_DELAY_S = LINE_DRAW_IN_DELAY_S + LINE_DRAW_IN_DURATION_S;
const BIRTH_COMPLETE_DELAY_MS = BIRTH_COMPLETE_DELAY_S * 1000;

const PULSE_HOP_DURATION_MS = 800;
const PULSE_MIN_GAP_MS = 4000;
const PULSE_GAP_JITTER_MS = 2000;

function pickPulsePath(nodeCount: number, edges: NetworkEdge[]): number[] {
  const adjacency = new Map<number, number[]>();
  edges.forEach(({ a, b }) => {
    adjacency.set(a, [...(adjacency.get(a) ?? []), b]);
    adjacency.set(b, [...(adjacency.get(b) ?? []), a]);
  });

  const hopCount = 2 + Math.floor(Math.random() * 2);
  let current = Math.floor(Math.random() * nodeCount);
  const path = [current];

  for (let hop = 0; hop < hopCount; hop += 1) {
    const neighbors = adjacency.get(current) ?? [];
    const previous = path.length > 1 ? path[path.length - 2] : undefined;
    const candidates = neighbors.filter((neighbor) => neighbor !== previous);
    const pool = candidates.length > 0 ? candidates : neighbors;
    if (pool.length === 0) {
      break;
    }
    current = pool[Math.floor(Math.random() * pool.length)];
    path.push(current);
  }

  return path;
}

type AmbientNetworkProps = { className?: string };

export function AmbientNetwork({ className }: AmbientNetworkProps) {
  const [pulses, setPulses] = useState<NetworkPulse[]>([]);

  // Single recursive timer schedules pulses one at a time (never one timer
  // per node). Each pulse's own lifetime is a second, short-lived timer that
  // clears itself; there are never more than a handful of these in flight.
  useEffect(() => {
    let nextId = 0;
    let scheduleTimeoutId: ReturnType<typeof setTimeout>;
    let cancelled = false;

    function scheduleNextPulse(extraDelayMs: number) {
      const gap = extraDelayMs + PULSE_MIN_GAP_MS + Math.random() * PULSE_GAP_JITTER_MS;
      scheduleTimeoutId = setTimeout(() => {
        if (cancelled) {
          return;
        }
        const path = pickPulsePath(NETWORK_NODES.length, NETWORK_EDGES);
        const id = nextId;
        nextId += 1;
        setPulses((current) => [...current, { id, path }]);

        const lifetime = (path.length - 1) * PULSE_HOP_DURATION_MS + 400;
        setTimeout(() => {
          if (cancelled) {
            return;
          }
          setPulses((current) => current.filter((pulse) => pulse.id !== id));
        }, lifetime);

        scheduleNextPulse(0);
      }, gap);
    }

    scheduleNextPulse(BIRTH_COMPLETE_DELAY_MS);

    return () => {
      cancelled = true;
      clearTimeout(scheduleTimeoutId);
    };
  }, []);

  return (
    <svg
      viewBox="0 0 400 800"
      preserveAspectRatio="xMidYMid slice"
      className={className ?? 'w-full h-full'}
      aria-hidden="true"
    >
      {NETWORK_EDGES.map((edge, edgeIndex) => {
        const nodeA = NETWORK_NODES[edge.a];
        const nodeB = NETWORK_NODES[edge.b];
        const lineDelay =
          LINE_DRAW_IN_DELAY_S +
          (edgeIndex / Math.max(NETWORK_EDGES.length - 1, 1)) * LINE_DRAW_IN_STAGGER_S;
        return (
          <line
            key={`edge-${edge.a}-${edge.b}`}
            x1={nodeA.x}
            y1={nodeA.y}
            x2={nodeB.x}
            y2={nodeB.y}
            stroke="var(--color-ink-subtle)"
            strokeOpacity={0.25}
            strokeWidth={1}
            style={{
              opacity: 0,
              animation: `line-draw-in ${LINE_DRAW_IN_DURATION_S}s ease-out ${lineDelay}s both`,
            }}
          />
        );
      })}

      {NETWORK_NODES.map((node, index) => {
        const fadeInDelay =
          (index / Math.max(NETWORK_NODES.length - 1, 1)) * NODE_FADE_IN_SPREAD_S;
        const breathDelay = BIRTH_COMPLETE_DELAY_S + node.delay;
        return (
          <circle
            key={index}
            cx={node.x}
            cy={node.y}
            r={node.r}
            fill="var(--color-brand)"
            style={
              {
                opacity: 0,
                '--dot-base-opacity': node.opacity,
                animation: `node-fade-in ${NODE_FADE_IN_DURATION_S}s ease-out ${fadeInDelay}s both, constellation-breath ${node.duration}s ease-in-out infinite ${breathDelay}s`,
                // SVG shapes transform around the viewport origin by default,
                // not their own center — without this, scale() would make
                // each node visibly drift toward (0,0) instead of pulsing in place.
                transformOrigin: `${node.x}px ${node.y}px`,
              } as CSSProperties
            }
          />
        );
      })}

      {pulses.map((pulse) => (
        <g key={pulse.id}>
          {pulse.path.slice(0, -1).map((fromIndex, hopIndex) => {
            const toIndex = pulse.path[hopIndex + 1];
            const from = NETWORK_NODES[fromIndex];
            const to = NETWORK_NODES[toIndex];
            return (
              <line
                key={`hop-line-${hopIndex}`}
                x1={from.x}
                y1={from.y}
                x2={to.x}
                y2={to.y}
                stroke="var(--color-brand-hover)"
                strokeLinecap="round"
                strokeWidth={1}
                style={{
                  opacity: 0,
                  animation: `pulse-line-surge ${PULSE_HOP_DURATION_MS}ms ease-in-out ${hopIndex * PULSE_HOP_DURATION_MS}ms both`,
                }}
              />
            );
          })}
          {pulse.path.map((nodeIndex, hopIndex) => {
            const node = NETWORK_NODES[nodeIndex];
            return (
              <circle
                key={`hop-node-${hopIndex}`}
                cx={node.x}
                cy={node.y}
                r={node.r}
                fill="var(--color-brand-hover)"
                style={
                  {
                    opacity: 0,
                    animation: `pulse-node-flash 400ms ease-in-out ${hopIndex * PULSE_HOP_DURATION_MS}ms both`,
                    transformOrigin: `${node.x}px ${node.y}px`,
                  } as CSSProperties
                }
              />
            );
          })}
        </g>
      ))}
    </svg>
  );
}
