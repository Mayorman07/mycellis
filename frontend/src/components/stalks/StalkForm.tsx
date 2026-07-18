import { useState, type CSSProperties, type FormEvent } from 'react';
import type { CreateStalkRequest } from '../../lib/types';
import { HINT_CLASSES, INPUT_CLASSES, LABEL_CLASSES } from '../../lib/formClasses';

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
//
// new URL() alone is too permissive — "https://google" (no TLD) parses fine,
// and users who submit that get a stalk whose first pulse fails DNS
// resolution and sits DEGRADED forever. Also require http(s) and a dotted
// hostname (or literal "localhost" for local dev targets).
function isValidUrlFormat(candidate: string): boolean {
  if (!candidate) {
    return false;
  }
  try {
    const parsed = new URL(candidate);
    const isHttpOrHttps = parsed.protocol === 'http:' || parsed.protocol === 'https:';
    const hasValidHost = parsed.hostname.includes('.') || parsed.hostname === 'localhost';
    return isHttpOrHttps && hasValidHost;
  } catch {
    return false;
  }
}

type StalkFormProps = {
  mode: 'create' | 'edit';
  initialValues?: Partial<CreateStalkRequest>;
  onSubmit: (values: CreateStalkRequest) => void;
  isSubmitting: boolean;
  errorMessage: string | null;
  onCancel: () => void;
  submitLabel: string;
  cancelLabel: string;
};

export function StalkForm({
  mode,
  initialValues,
  onSubmit,
  isSubmitting,
  errorMessage,
  onCancel,
  submitLabel,
  cancelLabel,
}: StalkFormProps) {
  const [nickname, setNickname] = useState(initialValues?.nickname ?? '');
  const [url, setUrl] = useState(initialValues?.url ?? '');
  const [timeoutSeconds, setTimeoutSeconds] = useState(
    initialValues?.timeoutSeconds ?? TIMEOUT_DEFAULT,
  );
  const [growthIntervalSeconds, setGrowthIntervalSeconds] = useState(
    initialValues?.growthIntervalSeconds ?? INTERVAL_DEFAULT,
  );

  const urlIsValid = isValidUrlFormat(url);
  const submittingLabel = mode === 'create' ? 'Planting…' : 'Saving…';

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit({ nickname, url, timeoutSeconds, growthIntervalSeconds });
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
    <>
      {errorMessage && (
        <div role="alert" className="mb-6 rounded-md border border-hairline bg-surface-raised px-4 py-3">
          <div className="flex items-start gap-2">
            <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-state-down flex-shrink-0" />
            <p className="text-sm text-ink">{errorMessage}</p>
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
          <p className={HINT_CLASSES}>
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
          <p className={HINT_CLASSES}>The exact endpoint you want us to hit.</p>
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
            <p className={HINT_CLASSES}>
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
            <p className={HINT_CLASSES}>How often to send a pulse.</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-md border border-hairline-strong bg-surface-raised px-6 py-4 text-sm font-medium text-ink hover:bg-surface-sunken"
          >
            {cancelLabel}
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            aria-busy={isSubmitting}
            className={`flex-1 rounded-md bg-brand px-4 py-4 text-sm font-medium text-brand-fg hover:bg-brand-hover disabled:opacity-60 ${
              isSubmitting ? 'cursor-default' : 'cursor-pointer'
            }`}
          >
            {isSubmitting ? submittingLabel : submitLabel}
          </button>
        </div>
      </form>
    </>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 20 20" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M4 10.5l3.5 3.5L16 6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
