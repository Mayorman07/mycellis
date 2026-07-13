import { useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { getTheme, setTheme } from '../lib/theme';
import { AmbientNetwork } from '../components/auth/AmbientNetwork';

type LocationState = { email?: string } | null;

export default function VerifyPendingPage() {
  const location = useLocation();

  // Same locked-cream, per-page mount/unmount pattern as LoginPage/SignupPage.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  const state = location.state as LocationState;
  const queryEmail = new URLSearchParams(location.search).get('email');
  const email = state?.email || queryEmail || null;

  const resendHref = email ? `/resend-verification?email=${encodeURIComponent(email)}` : '/resend-verification';

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

          <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-3">
            Check your email.
          </h1>

          {email ? (
            <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-6">
              We sent a verification link to <span className="text-ink font-medium">{email}</span>.
              Click the link to activate your account.
            </p>
          ) : (
            <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-6">
              Check your email for a verification link to activate your account.
            </p>
          )}

          <p className="text-sm text-ink-muted">
            Didn't get it? Check spam folder or{' '}
            <Link to={resendHref} className="text-ink hover:underline">
              resend verification
            </Link>
          </p>
        </div>
      </div>

      <div className="hidden lg:flex lg:w-[40%] overflow-hidden">
        <AmbientNetwork className="w-full h-full" />
      </div>
    </div>
  );
}
