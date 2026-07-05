import { useCallback, useState } from 'react';

export type Theme = 'cream' | 'white' | 'black';

const STORAGE_KEY = 'mycellis-theme';
const DEFAULT_THEME: Theme = 'cream';

function isTheme(value: string | null): value is Theme {
  return value === 'cream' || value === 'white' || value === 'black';
}

export function getTheme(): Theme {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return isTheme(stored) ? stored : DEFAULT_THEME;
  } catch {
    return DEFAULT_THEME;
  }
}

export function setTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme;
  try {
    localStorage.setItem(STORAGE_KEY, theme);
  } catch {
    // localStorage unavailable (private browsing, quota) — theme still applies for this session
  }
}

export function useTheme(): [Theme, (theme: Theme) => void] {
  const [theme, setThemeState] = useState<Theme>(getTheme);

  const set = useCallback((next: Theme) => {
    setTheme(next);
    setThemeState(next);
  }, []);

  return [theme, set];
}
