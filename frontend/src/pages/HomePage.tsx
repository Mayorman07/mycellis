import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getTheme, setTheme } from '../lib/theme';
import { useSession } from '../lib/hooks/useSession';

export default function HomePage() {
  // Same locked-cream pattern as GuidePage — this is a public page and
  // ignores whatever theme a logged-in visitor last set for themselves.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  const navigate = useNavigate();
  const { isLoading, isAuthenticated } = useSession();

  // Anonymous visitors (the common case) get the landing page with zero
  // wait — we don't gate the initial render on the session check. Already
  // logged-in visitors see it flash briefly, then get bounced to /dashboard
  // once the session query resolves.
  useEffect(() => {
    if (!isLoading && isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isLoading, isAuthenticated, navigate]);

  return (
    <div className="min-h-screen bg-surface flex flex-col">
      <header className="border-b border-hairline bg-surface px-6 py-4">
        <div className="max-w-2xl mx-auto flex items-center justify-between">
          <Link to="/" className="font-display text-xl text-ink tracking-tight">
            MYCELLIS
          </Link>
          <div className="flex items-center gap-4">
            <Link to="/login" className="text-sm text-ink-subtle hover:text-ink">
              Sign in
            </Link>
            <Link
              to="/signup"
              className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-brand-fg hover:bg-brand-hover"
            >
              Get started
            </Link>
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-2xl mx-auto px-6 py-24 md:py-32 text-center">
        <h1 className="font-display font-normal text-[46px] md:text-[56px] leading-tight tracking-tight text-ink mb-6">
          Watch your services breathe.
        </h1>
        <p className="text-lg text-ink-muted leading-[1.6] mb-10 max-w-md mx-auto">
          Mycellis watches every endpoint you rely on and tells you how each one is breathing —
          its health, its latency, the quiet moments and the failures.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-6">
          <Link
            to="/signup"
            className="w-full sm:w-auto rounded-md bg-brand px-6 py-3 text-sm font-medium text-brand-fg text-center hover:bg-brand-hover"
          >
            Get started →
          </Link>
          <Link
            to="/login"
            className="w-full sm:w-auto rounded-md border border-hairline-strong bg-surface-raised px-6 py-3 text-sm font-medium text-ink text-center hover:bg-surface-sunken"
          >
            Sign in
          </Link>
        </div>

        <Link to="/guide" className="text-sm text-ink-subtle hover:text-ink">
          Learn more in the 2-minute guide →
        </Link>
      </main>

      <footer className="px-6 py-8 text-center">
        <p className="text-xs text-ink-subtle">© 2026 Mycellis</p>
      </footer>
    </div>
  );
}
