/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        mycellis: {
          teal: '#0d7377',
          'teal-hover': '#0a5d60',
          aquamarine: '#7fffd4',
          ink: '#0a2e2f',
          'text-secondary': '#4a6566',
          'text-muted': '#8aa6a7',
          'bg-page': '#f1f5f5',
          'bg-card': '#ffffff',
          'bg-subtle': '#f5faf9',
          border: '#d8e8e8',
        },
        status: {
          healthy: '#22c55e',
          stressed: '#f59e0b',
          down: '#ef4444',
          dormant: '#94a3b8',
        },
      },
    },
  },
  plugins: [],
}