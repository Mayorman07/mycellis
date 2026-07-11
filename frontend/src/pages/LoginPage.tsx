import { useEffect, useState, type CSSProperties, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { login } from '../lib/api/auth';
import type { ApiError } from '../lib/api/client';
import { getTheme, setTheme } from '../lib/theme';

type LocationState = { from?: string } | null;

type LoginErrorMessage = {
  title: string;
  detail?: string;
  action?: string;
  actionHref?: string;
};

const INPUT_CLASSES =
  'w-full rounded-md border-[1.5px] border-hairline-strong bg-surface-sunken px-4 py-4 text-base tracking-tight text-ink placeholder:text-ink-subtle shadow-[inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent)] transition-all duration-[250ms] ease-in-out focus:outline-none focus:border-brand focus:[box-shadow:inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent),0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]';

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

export default function LoginPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const state = location.state as LocationState;
  const from = state?.from ?? '/dashboard';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [pulses, setPulses] = useState<NetworkPulse[]>([]);

  // Auth pages read as a calm, consistent front door regardless of the
  // visitor's dashboard theme preference — locked to cream while mounted,
  // restored the instant they navigate away. Per-page, not global: each
  // auth page owns this decision independently.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

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

  const loginMutation = useMutation<
    Awaited<ReturnType<typeof login>>,
    ApiError,
    Parameters<typeof login>[0]
  >({
    mutationFn: login,
    onSuccess: () => {
      // Session context is stale until this refetches — must happen before
      // navigating, or the destination route's ProtectedRoute check would
      // still see the old (unauthenticated) session state.
      queryClient.invalidateQueries({ queryKey: ['me'] });
      navigate(from, { replace: true });
    },
  });

  const errorMessage = deriveErrorMessage(loginMutation.error, email);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    loginMutation.mutate({ email, password, rememberMe });
  }

  return (
    <div className="h-screen flex bg-surface">
      <div className="w-full lg:w-[60%] relative overflow-y-auto flex flex-col justify-center px-8 sm:px-16 py-12 bg-[radial-gradient(ellipse_at_center,transparent_0%,color-mix(in_srgb,var(--color-ink)_2%,transparent)_100%)]">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-1/2 bg-[linear-gradient(135deg,color-mix(in_srgb,var(--color-brand)_4%,transparent)_0%,transparent_60%)]"
        />

        <div className="relative max-w-md w-full mx-auto lg:mx-0">
          <div className="mb-8">
            <p className="font-mono uppercase tracking-widest text-sm font-medium text-ink">
              MYCELLIS
            </p>
            <p className="font-mono tracking-wider text-xs font-normal text-ink-muted mt-0.5">
              Digital Ecology Monitor
            </p>
          </div>

          <h1 className="font-display font-normal text-[54px] leading-tight tracking-tight text-ink mb-3">
            Welcome back.
          </h1>
          <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-8">
            Your systems are waiting. Monitor every endpoint from one place.
          </p>

          {errorMessage && (
            <div
              role="alert"
              className="mb-6 rounded-md border border-hairline border-l-4 border-l-state-down bg-surface-raised px-4 py-3"
            >
              <div className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-state-down flex-shrink-0" />
                <div>
                  <p className="text-sm text-ink">
                    {errorMessage.title}
                    {errorMessage.action && errorMessage.actionHref && (
                      <>
                        {' '}
                        <Link to={errorMessage.actionHref} className="underline">
                          {errorMessage.action}
                        </Link>
                      </>
                    )}
                  </p>
                  {errorMessage.detail && (
                    <p className="text-xs text-ink-subtle mt-1">{errorMessage.detail}</p>
                  )}
                </div>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label
                htmlFor="email"
                className="block font-mono uppercase text-xs tracking-wider text-ink-subtle mb-2"
              >
                Email
              </label>
              <input
                id="email"
                type="email"
                required
                autoComplete="email"
                inputMode="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@company.com"
                className={INPUT_CLASSES}
              />
            </div>

            <div className="mb-6">
              <label
                htmlFor="password"
                className="block font-mono uppercase text-xs tracking-wider text-ink-subtle mb-2"
              >
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="••••••••"
                  className={`${INPUT_CLASSES} pr-12`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((current) => !current)}
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  aria-pressed={showPassword}
                  className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-ink-muted hover:text-ink"
                >
                  {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
            </div>

            <label className="flex items-start gap-3 mb-6 cursor-pointer">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(event) => setRememberMe(event.target.checked)}
                className="peer sr-only"
              />
              <span
                aria-hidden="true"
                className={`mt-0.5 flex-shrink-0 w-[18px] h-[18px] rounded border flex items-center justify-center transition-colors duration-150 ease-in-out peer-focus-visible:[box-shadow:0_0_0_3px_color-mix(in_srgb,var(--color-brand)_15%,transparent)] ${
                  rememberMe ? 'bg-brand border-brand' : 'bg-surface-sunken border-hairline'
                }`}
              >
                {rememberMe && (
                  <svg
                    viewBox="0 0 10 10"
                    width="10"
                    height="10"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.75"
                    className="text-brand-fg"
                  >
                    <path d="M1.5 5.2l2.4 2.4 4.6-5.2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                )}
              </span>
              <span>
                <span className="block text-sm text-ink">Trust this device for 30 days</span>
                <span className="block text-xs text-ink-subtle">Stays logged in on this device.</span>
              </span>
            </label>

            <button
              type="submit"
              disabled={loginMutation.isPending}
              className="w-full rounded-md bg-brand px-4 py-3 text-sm font-medium text-brand-fg disabled:opacity-60"
            >
              {loginMutation.isPending ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <div className="flex items-center justify-between mt-6 text-sm text-ink-muted">
            <Link to="/signup" className="hover:text-ink">
              Create account
            </Link>
            <Link to="/forgot-password" className="hover:text-ink">
              Forgot password?
            </Link>
          </div>
        </div>
      </div>

      <div className="hidden lg:flex lg:w-[40%] overflow-hidden">
        <svg
          viewBox="0 0 400 800"
          preserveAspectRatio="xMidYMid slice"
          className="w-full h-full"
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
      </div>
    </div>
  );
}

function deriveErrorMessage(error: ApiError | null, email: string): LoginErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers. Check your connection and try again." };
  }

  if (error.status === 403 && error.type?.includes('account-not-verified')) {
    return {
      title: 'Verify your email before signing in.',
      action: 'Resend verification email',
      actionHref: `/resend-verification?email=${encodeURIComponent(email)}`,
    };
  }

  if (error.status === 429) {
    return { title: 'Too many attempts. Try again in a few minutes.' };
  }

  if (error.status === 400 || error.status === 401) {
    return { title: 'Email or password is incorrect. Try again.' };
  }

  return {
    title: 'Something went wrong. Please try again.',
    detail: error.title || undefined,
  };
}

function EyeIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path
        d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12" r="3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path
        d="M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 7 11 7a18.5 18.5 0 0 1-2.16 3.19M6.61 6.61C3.06 8.72 1 12 1 12s4 7 11 7a10.9 10.9 0 0 0 5.39-1.61M14.12 14.12a3 3 0 1 1-4.24-4.24"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M1 1l22 22" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
