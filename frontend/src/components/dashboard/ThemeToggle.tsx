import { useTheme, cycleTheme } from '../../lib/theme';

// White theme is the "day mode" but reads with a half-moon icon, not a sun —
// the sun is reserved for cream, our actual default/warmest theme.
export function ThemeToggle() {
  const [theme, setTheme] = useTheme();

  return (
    <button
      type="button"
      onClick={() => setTheme(cycleTheme(theme))}
      aria-label="Cycle theme"
      className="flex items-center justify-center w-8 h-8 rounded-md border border-hairline bg-surface-raised text-ink"
    >
      {theme === 'cream' && <SunIcon />}
      {theme === 'white' && <HalfMoonIcon />}
      {theme === 'black' && <MoonIcon />}
    </button>
  );
}

function SunIcon() {
  return (
    <svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round">
      <circle cx="10" cy="10" r="3.5" />
      <path d="M10 1.5v2M10 16.5v2M1.5 10h2M16.5 10h2M4.2 4.2l1.4 1.4M14.4 14.4l1.4 1.4M15.8 4.2l-1.4 1.4M5.6 14.4l-1.4 1.4" />
    </svg>
  );
}

function HalfMoonIcon() {
  return (
    <svg viewBox="0 0 20 20" width="16" height="16">
      <circle cx="10" cy="10" r="7.5" stroke="currentColor" strokeWidth="1.5" fill="none" />
      <path d="M10 2.5a7.5 7.5 0 0 1 0 15z" fill="currentColor" />
    </svg>
  );
}

function MoonIcon() {
  return (
    <svg viewBox="0 0 20 20" width="16" height="16" fill="currentColor">
      <path d="M15.5 12.5A7 7 0 0 1 7.5 4.5a7 7 0 1 0 8 8z" />
    </svg>
  );
}
