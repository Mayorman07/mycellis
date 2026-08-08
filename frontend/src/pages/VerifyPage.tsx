import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { verifyEmail } from '../lib/api/auth';
import { ApiError } from '../lib/api/client';
import { getTheme, setTheme } from '../lib/theme';
import { AmbientNetwork } from '../components/auth/AmbientNetwork';
import { MycellisFlowerLoader } from '../components/shared/MycellisFlowerLoader';

type VerifyState =
  | { kind: 'missing-token' }
  | { kind: 'loading' }
  | { kind: 'success' }
  | { kind: 'error-token' }
  | { kind: 'error-network' };

type StateContent = {
  headline: string;
  subhead: string | null;
  action: { label: string; href: string } | null;
};

function getStateContent(state: VerifyState): StateContent {
  switch (state.kind) {
    case 'missing-token':
      return {
        headline: 'This link looks incomplete.',
        subhead:
          'The verification link needs to include a token. Try clicking the link in your email again, or request a new one.',
        action: { label: 'Request a new link', href: '/resend-verification' },
      };
    case 'loading':
      return { headline: 'Verifying your email...', subhead: null, action: null };
    case 'success':
      return {
        headline: "You're verified.",
        subhead: 'Welcome to Mycellis. Sign in to plant your first stalk.',
        action: { label: 'Sign in', href: '/login' },
      };
    case 'error-token':
      return {
        headline: 'This link has expired or been used already.',
        subhead: 'Get a new verification link and try again.',
        action: { label: 'Request a new link', href: '/resend-verification' },
      };
    case 'error-network':
      return {
        headline: 'Something went wrong.',
        subhead: "Couldn't reach our servers. Try again in a moment.",
        action: null,
      };
  }
}

export default function VerifyPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [state, setState] = useState<VerifyState>(
    token ? { kind: 'loading' } : { kind: 'missing-token' },
  );

  // Same locked-cream, per-page mount/unmount pattern as the other auth pages.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  // Verification tokens are single-use — a second call with the same token
  // 404s as "already used". React StrictMode double-invokes effects in dev,
  // which would otherwise fire this twice and make a genuinely successful
  // verification look like a failure on the second (real) network call.
  // This ref makes the actual request fire at most once per token,
  // regardless of how many times the effect body itself runs.
  const hasRequested = useRef(false);

  useEffect(() => {
    if (!token || hasRequested.current) {
      return;
    }
    hasRequested.current = true;

    verifyEmail({ token })
      .then(() => setState({ kind: 'success' }))
      .catch((error: unknown) => {
        if (error instanceof ApiError && (error.status === 404 || error.status === 401)) {
          setState({ kind: 'error-token' });
          return;
        }
        setState({ kind: 'error-network' });
      });
  }, [token]);

  const content = getStateContent(state);
  const isLoading = state.kind === 'loading';

  return (
    <div className="h-screen overflow-y-auto scrollbar-none flex bg-surface">
      <div className="w-full lg:w-[60%] relative flex flex-col justify-center px-8 sm:px-16 py-12 bg-[radial-gradient(ellipse_at_center,transparent_0%,color-mix(in_srgb,var(--color-ink)_2%,transparent)_100%)]">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-1/2 bg-[linear-gradient(135deg,color-mix(in_srgb,var(--color-brand)_4%,transparent)_0%,transparent_60%)]"
        />

        <div className="relative max-w-md w-full mx-auto lg:mx-0">
          <div className="mb-8">
            <p className="font-mono uppercase tracking-widest text-sm font-semibold text-ink">
              MYCELLIS
            </p>
            <p className="font-mono tracking-wider text-xs font-normal text-ink-muted mt-0.5">
              Digital Ecology Monitor
            </p>
          </div>

          <div aria-live="polite" aria-busy={isLoading}>
            <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-3">
              {content.headline}
            </h1>

            {isLoading ? (
              <div className="mb-8">
                <MycellisFlowerLoader size="md" />
              </div>
            ) : (
              content.subhead && (
                <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-8">
                  {content.subhead}
                </p>
              )
            )}

            {content.action && (
              <Link
                to={content.action.href}
                className="inline-block w-full sm:w-auto rounded-md bg-brand px-4 py-3 text-sm font-medium text-brand-fg text-center hover:bg-brand-hover"
              >
                {content.action.label}
              </Link>
            )}

            {state.kind === 'error-network' && (
              <button
                type="button"
                onClick={() => window.location.reload()}
                className="w-full sm:w-auto rounded-md border-[1.5px] border-[color-mix(in_srgb,var(--color-ink)_25%,transparent)] px-4 py-3 text-sm font-medium text-ink hover:bg-surface-sunken"
              >
                Try again
              </button>
            )}
          </div>

          <div className="mt-6 text-sm text-ink-muted">
            Already verified?{' '}
            <Link to="/login" className="text-ink hover:underline">
              Sign in
            </Link>
          </div>
        </div>
      </div>

      <div className="hidden lg:flex lg:w-[40%] sticky top-0 h-screen overflow-hidden">
        <AmbientNetwork className="w-full h-full" />
      </div>
    </div>
  );
}
