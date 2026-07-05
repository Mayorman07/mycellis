// Full component is deleted in Commit 4 when the real Sparkline arrives.
export function SparklinePlaceholder() {
  return (
    <div
      className="w-full h-5 rounded"
      // Tailwind's opacity modifier can't decompose our CSS-variable-based
      // ink color into channels, so the faint tint is mixed manually here.
      style={{ backgroundColor: 'color-mix(in srgb, var(--color-ink) 5%, transparent)' }}
    />
  );
}
