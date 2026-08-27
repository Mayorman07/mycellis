import { useEffect, useState } from 'react';

/**
 * Live-updating seconds-remaining countdown, ticking down from
 * retryAfterSeconds to 0 once per second. Returns 0 immediately for null
 * (nothing to count down) or once the countdown has elapsed.
 *
 * Pure primitive, deliberately decoupled from ApiError — callers extract
 * whatever number they have (e.g. error?.retryAfterSeconds ?? null) and
 * pass it in directly, same "small composable helper" shape as
 * getApiErrorMessage.
 */
export function useRetryCountdown(retryAfterSeconds: number | null): number {
  // Resets secondsRemaining when retryAfterSeconds changes — done here,
  // directly during render (React's documented pattern for "adjusting state
  // when a prop changes"), rather than via a setState call inside the effect
  // below, which would trigger react-hooks/set-state-in-effect.
  const [prevRetryAfterSeconds, setPrevRetryAfterSeconds] = useState(retryAfterSeconds);
  const [secondsRemaining, setSecondsRemaining] = useState(() => retryAfterSeconds ?? 0);

  if (retryAfterSeconds !== prevRetryAfterSeconds) {
    setPrevRetryAfterSeconds(retryAfterSeconds);
    setSecondsRemaining(retryAfterSeconds ?? 0);
  }

  useEffect(() => {
    if (!retryAfterSeconds || retryAfterSeconds <= 0) {
      return;
    }

    const id = setInterval(() => {
      setSecondsRemaining((current) => {
        if (current <= 1) {
          clearInterval(id);
          return 0;
        }
        return current - 1;
      });
    }, 1000);

    return () => clearInterval(id);
  }, [retryAfterSeconds]);

  return secondsRemaining;
}
