import type { CSSProperties } from 'react';

// Shared form field styling for auth pages and StalkForm — extracted from
// what was duplicated verbatim across 6 auth pages + StalkForm. Keep any
// visual change here deliberate: this is consumed by every form in the app.

export const INPUT_CLASSES =
  'w-full rounded-md border-[1.5px] border-[color-mix(in_srgb,var(--color-ink)_25%,transparent)] bg-surface-sunken px-4 py-4 text-base tracking-tight text-ink placeholder:text-ink-subtle shadow-[inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent)] transition-all duration-[250ms] ease-in-out focus:outline-none focus:border-brand focus:[box-shadow:inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent),0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]';

export const LABEL_CLASSES = 'block font-mono uppercase text-xs tracking-wider text-ink-subtle mb-2';

export const HINT_CLASSES = 'mt-2 text-xs text-ink-subtle';

// Applied as an inline style (not a class) alongside INPUT_CLASSES when a
// field fails validation — INPUT_CLASSES already sets border-color via an
// arbitrary-value utility, and Tailwind's generated-CSS ordering isn't
// guaranteed to let a second border-color class win over it. An inline
// style always wins, so it's the reliable way to override just that one
// property without touching the shared base classes.
export const INPUT_ERROR_STYLE: CSSProperties = {
  borderColor: 'color-mix(in srgb, var(--color-state-down) 60%, transparent)',
};
