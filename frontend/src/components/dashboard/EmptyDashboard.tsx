import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

// Same petal geometry as MycellisFlowerLoader — copied, not imported, since
// this is a larger, differently-animated variant (a single slow whole-unit
// breathing scale, not the loader's per-petal bloom/close cycle).
const HERO_PETAL_PATH = 'M 50,47 C 59,42 59,26 55,17 C 53,12.5 47,12.5 45,17 C 41,26 41,42 50,47 Z';
const HERO_PETAL_ANGLES = [0, 72, 144, 216, 288];
const HERO_PETAL_COLOR = '#eccb52';

type EmptyDashboardProps = {
  orgName: string;
  onCreateStalk: () => void;
};

export function EmptyDashboard({ orgName, onCreateStalk }: EmptyDashboardProps) {
  return (
    <div className="flex flex-col items-center text-center py-24 px-6">
      <EmptyStateFlower />

      <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-4">
        Welcome to {orgName}
      </p>

      <h1 className="font-display text-[52px] leading-tight tracking-tight text-ink mb-6">
        Nothing is being watched — <em className="italic">yet</em>.
      </h1>

      <p className="max-w-[500px] text-ink-muted leading-relaxed mb-8">
        Mycellis watches every endpoint you rely on and reports how each one is breathing —
        its health, its latency, its vitals, the quiet moments and the failures. Plant your
        first stalk and we'll start listening within seconds.
      </p>

      <div className="flex items-center gap-6">
        <button
          type="button"
          onClick={onCreateStalk}
          className="rounded-md bg-brand px-6 py-3 text-sm font-medium text-brand-fg"
        >
          + Plant your first stalk
        </button>
        <Link to="/guide" className="text-sm text-ink-subtle">
          Read the 2-minute guide →
        </Link>
      </div>
    </div>
  );
}

function usePrefersReducedMotion(): boolean {
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(
    () => window.matchMedia('(prefers-reduced-motion: reduce)').matches
  );

  useEffect(() => {
    const query = window.matchMedia('(prefers-reduced-motion: reduce)');
    function handleChange(event: MediaQueryListEvent) {
      setPrefersReducedMotion(event.matches);
    }
    query.addEventListener('change', handleChange);
    return () => query.removeEventListener('change', handleChange);
  }, []);

  return prefersReducedMotion;
}

function EmptyStateFlower() {
  const prefersReducedMotion = usePrefersReducedMotion();

  return (
    <svg
      viewBox="0 0 100 100"
      aria-hidden="true"
      className="w-[100px] h-[100px] md:w-[140px] md:h-[140px] mb-8"
      style={
        prefersReducedMotion
          ? undefined
          : { animation: 'mycellis-hero-breathe 4s cubic-bezier(0.65, 0, 0.35, 1) infinite' }
      }
    >
      {HERO_PETAL_ANGLES.map((angle) => (
        <g key={angle} transform={`rotate(${angle} 50 50)`}>
          <path d={HERO_PETAL_PATH} fill={HERO_PETAL_COLOR} />
        </g>
      ))}
      <circle cx="50" cy="50" r="8" fill="var(--color-brand)" />
    </svg>
  );
}
