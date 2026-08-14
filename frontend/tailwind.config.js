/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        surface: {
          DEFAULT: 'var(--color-surface)',
          raised: 'var(--color-surface-raised)',
          sunken: 'var(--color-surface-sunken)',
        },
        ink: {
          DEFAULT: 'var(--color-ink)',
          muted: 'var(--color-ink-muted)',
          subtle: 'var(--color-ink-subtle)',
        },
        hairline: {
          DEFAULT: 'var(--color-hairline)',
          strong: 'var(--color-hairline-strong)',
        },
        brand: {
          DEFAULT: 'var(--color-brand)',
          hover: 'var(--color-brand-hover)',
          fg: 'var(--color-brand-fg)',
        },
        accent: 'var(--color-accent)',
        state: {
          awakening: 'var(--color-state-awakening)',
          healthy: 'var(--color-state-healthy)',
          stressed: 'var(--color-state-stressed)',
          down: 'var(--color-state-down)',
          dormant: 'var(--color-state-dormant)',
        },
      },
      fontFamily: {
        display: ['Fraunces', 'serif'],
        mono: ['"JetBrains Mono"', 'monospace'],
      },
    },
  },
  plugins: [],
}
