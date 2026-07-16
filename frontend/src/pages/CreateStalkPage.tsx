import { useState, type CSSProperties, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createStalk } from '../lib/api/stalks';
import type { ApiError } from '../lib/api/client';

type CreateStalkErrorMessage = { title: string };

const INPUT_CLASSES =
  'w-full rounded-md border-[1.5px] border-[color-mix(in_srgb,var(--color-ink)_25%,transparent)] bg-surface-sunken px-4 py-4 text-base tracking-tight text-ink placeholder:text-ink-subtle shadow-[inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent)] transition-all duration-[250ms] ease-in-out focus:outline-none focus:border-brand focus:[box-shadow:inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent),0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]';

const LABEL_CLASSES = 'block font-mono uppercase text-xs tracking-wider text-ink-subtle mb-2';

// Backend's CreateStalkRequest enforces @Min(5)/@Max(120) on timeoutSeconds,
// but StalkServiceImpl additionally rejects anything above the scheduler's
// configured maxCycleDuration (mycelis.monitoring.max-cycle-duration,
// currently 30s) — a runtime ceiling tighter than the DTO's static @Max.
// 30 here matches that current config, not the DTO's 120.
const TIMEOUT_MIN = 5;
const TIMEOUT_MAX = 30;
const TIMEOUT_DEFAULT = 5;
const TIMEOUT_STEP = 1;

// Backend's @Min(10)/@Max(86400) on growthIntervalSeconds is far wider than
// this — 30s-1h is a deliberately narrower, more sensible range for the UI;
// nothing in it can be rejected by the backend.
const INTERVAL_MIN = 30;
const INTERVAL_MAX = 3600;
const INTERVAL_DEFAULT = 60;
const INTERVAL_STEP = 30;

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

function toPercent(value: number, min: number, max: number): number {
  return ((clamp(value, min, max) - min) / (max - min)) * 100;
}

// Format validation only — no ping, no backend round-trip. Pure function of
// `url`, so it's computed during render rather than synced into state via an
// effect (an effect here would just be redundant re-derivation on a delay).
function isValidUrlFormat(candidate: string): boolean {
  if (!candidate) {
    return false;
  }
  try {
    new URL(candidate);
    return true;
  } catch {
    return false;
  }
}

export default function CreateStalkPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [nickname, setNickname] = useState('');
  const [url, setUrl] = useState('');
  const [timeoutSeconds, setTimeoutSeconds] = useState(TIMEOUT_DEFAULT);
  const [growthIntervalSeconds, setGrowthIntervalSeconds] = useState(INTERVAL_DEFAULT);

  const urlIsValid = isValidUrlFormat(url);

  const createMutation = useMutation<
    Awaited<ReturnType<typeof createStalk>>,
    ApiError,
    Parameters<typeof createStalk>[0]
  >({
    mutationFn: createStalk,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stalks'] });
      navigate('/dashboard', { replace: true });
    },
  });

  const errorMessage = deriveErrorMessage(createMutation.error);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    createMutation.mutate({ nickname, url, timeoutSeconds, growthIntervalSeconds });
  }

  function handleTimeoutInputChange(value: string) {
    const parsed = Number(value);
    if (!Number.isNaN(parsed)) {
      setTimeoutSeconds(parsed);
    }
  }

  function handleIntervalInputChange(value: string) {
    const parsed = Number(value);
    if (!Number.isNaN(parsed)) {
      setGrowthIntervalSeconds(parsed);
    }
  }

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-6xl mx-auto px-6 py-12">
        <div className="max-w-[560px] mx-auto">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-4">
            MYCELLIS · NEW STALK
          </p>
          <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-3">
            Plant a new stalk.
          </h1>
          <p className="max-w-[500px] text-ink-muted leading-[1.5] mb-10">
            Point Mycellis at any endpoint. We'll listen and tell you how it's breathing.
          </p>

          {errorMessage && (
            <div
              role="alert"
              className="mb-6 rounded-md border border-hairline bg-surface-raised px-4 py-3"
            >
              <div className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-state-down flex-shrink-0" />
                <p className="text-sm text-ink">{errorMessage.title}</p>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="flex flex-col gap-6">
            <div>
              <label htmlFor="nickname" className={LABEL_CLASSES}>
                Nickname
              </label>
              <input
                id="nickname"
                type="text"
                required
                minLength={2}
                maxLength={50}
                value={nickname}
                onChange={(event) => setNickname(event.target.value)}
                placeholder="Stripe API"
                className={INPUT_CLASSES}
              />
              <p className="mt-2 text-xs text-ink-subtle">
                A short name you'll recognize in your dashboard.
              </p>
            </div>

            <div>
              <label htmlFor="url" className={LABEL_CLASSES}>
                URL
              </label>
              <div className="relative">
                <input
                  id="url"
                  type="url"
                  required
                  value={url}
                  onChange={(event) => setUrl(event.target.value)}
                  placeholder="https://api.stripe.com/v1/health"
                  className={`${INPUT_CLASSES} ${urlIsValid ? 'pr-12' : ''}`}
                />
                {urlIsValid && (
                  <span
                    aria-hidden="true"
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-state-healthy"
                  >
                    <CheckIcon />
                  </span>
                )}
              </div>
              <p className="mt-2 text-xs text-ink-subtle">
                The exact endpoint you want us to hit.
              </p>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div>
                <label htmlFor="timeoutSeconds" className={LABEL_CLASSES}>
                  Timeout (seconds)
                </label>
                <div className="flex items-center gap-4">
                  <input
                    id="timeoutSeconds"
                    type="range"
                    className="stalk-slider"
                    min={TIMEOUT_MIN}
                    max={TIMEOUT_MAX}
                    step={TIMEOUT_STEP}
                    value={clamp(timeoutSeconds, TIMEOUT_MIN, TIMEOUT_MAX)}
                    onChange={(event) => setTimeoutSeconds(Number(event.target.value))}
                    aria-label={`Timeout, ${timeoutSeconds} seconds`}
                    style={
                      {
                        '--range-progress': `${toPercent(timeoutSeconds, TIMEOUT_MIN, TIMEOUT_MAX)}%`,
                      } as CSSProperties
                    }
                  />
                  <input
                    type="number"
                    inputMode="numeric"
                    aria-label={`Timeout in seconds, currently ${timeoutSeconds}`}
                    value={timeoutSeconds}
                    onChange={(event) => handleTimeoutInputChange(event.target.value)}
                    onBlur={() =>
                      setTimeoutSeconds((current) => clamp(current, TIMEOUT_MIN, TIMEOUT_MAX))
                    }
                    className="w-20 flex-shrink-0 rounded-md border-[1.5px] border-[color-mix(in_srgb,var(--color-ink)_25%,transparent)] bg-surface-sunken px-2 py-2 text-right text-sm text-ink focus:outline-none focus:border-brand focus:[box-shadow:0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]"
                  />
                </div>
                <p className="mt-2 text-xs text-ink-subtle">
                  How long to wait for a response before marking a pulse as failed.
                </p>
              </div>

              <div>
                <label htmlFor="growthIntervalSeconds" className={LABEL_CLASSES}>
                  Check every (seconds)
                </label>
                <div className="flex items-center gap-4">
                  <input
                    id="growthIntervalSeconds"
                    type="range"
                    className="stalk-slider"
                    min={INTERVAL_MIN}
                    max={INTERVAL_MAX}
                    step={INTERVAL_STEP}
                    value={clamp(growthIntervalSeconds, INTERVAL_MIN, INTERVAL_MAX)}
                    onChange={(event) => setGrowthIntervalSeconds(Number(event.target.value))}
                    aria-label={`Check every, ${growthIntervalSeconds} seconds`}
                    style={
                      {
                        '--range-progress': `${toPercent(growthIntervalSeconds, INTERVAL_MIN, INTERVAL_MAX)}%`,
                      } as CSSProperties
                    }
                  />
                  <input
                    type="number"
                    inputMode="numeric"
                    aria-label={`Check every in seconds, currently ${growthIntervalSeconds}`}
                    value={growthIntervalSeconds}
                    onChange={(event) => handleIntervalInputChange(event.target.value)}
                    onBlur={() =>
                      setGrowthIntervalSeconds((current) => clamp(current, INTERVAL_MIN, INTERVAL_MAX))
                    }
                    className="w-20 flex-shrink-0 rounded-md border-[1.5px] border-[color-mix(in_srgb,var(--color-ink)_25%,transparent)] bg-surface-sunken px-2 py-2 text-right text-sm text-ink focus:outline-none focus:border-brand focus:[box-shadow:0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]"
                  />
                </div>
                <p className="mt-2 text-xs text-ink-subtle">How often to send a pulse.</p>
              </div>
            </div>

            <button
              type="submit"
              disabled={createMutation.isPending}
              aria-busy={createMutation.isPending}
              className={`w-full rounded-md bg-brand px-4 py-4 text-sm font-medium text-brand-fg hover:bg-brand-hover disabled:opacity-60 ${
                createMutation.isPending ? 'cursor-default' : 'cursor-pointer'
              }`}
            >
              {createMutation.isPending ? 'Planting…' : 'Plant stalk'}
            </button>
          </form>

          <div className="mt-6">
            <Link to="/dashboard" className="text-sm text-ink-muted hover:text-ink">
              Cancel and go back
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}

function deriveErrorMessage(error: ApiError | null): CreateStalkErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers. Check your connection." };
  }

  if (error.status === 429) {
    return { title: "You've created a lot of stalks recently. Try again in a moment." };
  }

  if (error.status === 400) {
    return { title: error.detail || 'Please check your inputs and try again.' };
  }

  return { title: 'Something went wrong. Please try again.' };
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 20 20" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M4 10.5l3.5 3.5L16 6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
