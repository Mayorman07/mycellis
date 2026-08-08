import { useEffect, useState } from 'react';

type MycellisFlowerLoaderProps = {
  size?: 'sm' | 'md' | 'lg';
  label?: string;
};

const SIZE_PX: Record<NonNullable<MycellisFlowerLoaderProps['size']>, number> = {
  sm: 24,
  md: 48,
  lg: 96,
};

// Between #e8c547 and #f0d060 — tuned toward the more saturated end so it
// still reads clearly against the cream surface (#efe6d0) rather than
// washing out.
const PETAL_COLOR = '#eccb52';

const PETAL_ANGLES = [0, 72, 144, 216, 288];

// Drawn in the flower's own 0-100 viewBox space, base near the center
// (50,50) and tip pointing straight up — a plump teardrop (wide belly,
// narrow-ish base, rounded tip) rather than a symmetric almond/eye shape.
// Each <g> below rotates a copy of this same path around the center point
// to place the 5 petals at 72-degree intervals.
const PETAL_PATH = 'M 50,47 C 59,42 59,26 55,17 C 53,12.5 47,12.5 45,17 C 41,26 41,42 50,47 Z';

function usePrefersReducedMotion(): boolean {
  // Lazy initializer computes the starting value directly during first
  // render — the effect below only needs to subscribe to future changes,
  // not set the initial value itself.
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

/**
 * A stylized 5-petal Mycellis flower (Mycelis muralis, wall lettuce — the
 * app's namesake) that blooms and closes on a loop. Pure inline SVG, no
 * external assets.
 *
 * Petals emerge from and scale around the flower's center point (50,50 in
 * the SVG's own viewBox coordinates) — each petal's transform-origin is set
 * to that fixed point, not its own bounding-box center, so it visibly grows
 * outward from the middle rather than expanding symmetrically in place.
 * Static positioning (the 72-degree rotation) lives on a wrapping <g> per
 * petal, separate from the animated scale/opacity on the <path> itself,
 * since a single element can only carry one `transform` — stacking a CSS
 * animation directly on a rotated element would silently replace the
 * rotation instead of composing with it.
 */
export function MycellisFlowerLoader({ size = 'md', label = 'Loading' }: MycellisFlowerLoaderProps) {
  const prefersReducedMotion = usePrefersReducedMotion();
  const pixelSize = SIZE_PX[size];

  return (
    <div
      role="status"
      aria-label={label}
      aria-live={prefersReducedMotion ? 'polite' : undefined}
      className="inline-flex items-center justify-center"
    >
      <svg width={pixelSize} height={pixelSize} viewBox="0 0 100 100" aria-hidden="true">
        {PETAL_ANGLES.map((angle) => (
          <g key={angle} transform={`rotate(${angle} 50 50)`}>
            <path
              d={PETAL_PATH}
              fill={PETAL_COLOR}
              style={{
                transformOrigin: '50px 50px',
                ...(prefersReducedMotion
                  ? {}
                  : { animation: 'mycellis-flower-bloom 2.6s cubic-bezier(0.65, 0, 0.35, 1) infinite' }),
              }}
            />
          </g>
        ))}
        <circle cx="50" cy="50" r="8" fill="var(--color-brand, #1c4d3a)" />
      </svg>
    </div>
  );
}
