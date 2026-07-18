import { useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { resendVerification } from '../lib/api/auth';
import type { ApiError } from '../lib/api/client';
import { getTheme, setTheme } from '../lib/theme';
import { AmbientNetwork } from '../components/auth/AmbientNetwork';
import { INPUT_CLASSES, LABEL_CLASSES } from '../lib/formClasses';

type ResendErrorMessage = { title: string };

export default function ResendVerificationPage() {
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState(searchParams.get('email') ?? '');

  // Same locked-cream, per-page mount/unmount pattern as the other auth pages.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  const resendMutation = useMutation<
    Awaited<ReturnType<typeof resendVerification>>,
    ApiError,
    Parameters<typeof resendVerification>[0]
  >({
    mutationFn: resendVerification,
  });

  const errorMessage = deriveErrorMessage(resendMutation.error);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    resendMutation.mutate({ email });
  }

  function handleTryAgain() {
    resendMutation.reset();
  }

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

          <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-3">
            Get a new verification link.
          </h1>

          {resendMutation.isSuccess ? (
            <>
              <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-6">
                If your email is unverified, we sent a new link. Check your inbox and spam folder.
              </p>
              <p className="text-sm text-ink-muted">
                Didn't get it?{' '}
                <button
                  type="button"
                  onClick={handleTryAgain}
                  className="text-ink hover:underline"
                >
                  Try again
                </button>
              </p>
            </>
          ) : (
            <>
              <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-8">
                Enter your email and we'll send a new one.
              </p>

              {errorMessage && (
                <div
                  role="alert"
                  className="mb-6 rounded-md border border-hairline border-l-4 border-l-state-down bg-surface-raised px-4 py-3"
                >
                  <div className="flex items-start gap-2">
                    <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-state-down flex-shrink-0" />
                    <p className="text-sm text-ink">{errorMessage.title}</p>
                  </div>
                </div>
              )}

              <form onSubmit={handleSubmit}>
                <div className="mb-8">
                  <label htmlFor="email" className={LABEL_CLASSES}>
                    Email
                  </label>
                  <input
                    id="email"
                    type="email"
                    required
                    autoComplete="email"
                    inputMode="email"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="you@company.com"
                    className={INPUT_CLASSES}
                  />
                </div>

                <button
                  type="submit"
                  disabled={resendMutation.isPending}
                  aria-busy={resendMutation.isPending}
                  className="w-full rounded-md bg-brand px-4 py-3 text-sm font-medium text-brand-fg disabled:opacity-60"
                >
                  {resendMutation.isPending ? 'Sending…' : 'Send new link'}
                </button>
              </form>

              <div className="mt-6 text-sm text-ink-muted">
                Already verified?{' '}
                <Link to="/login" className="text-ink hover:underline">
                  Sign in
                </Link>
              </div>
            </>
          )}
        </div>
      </div>

      <div className="hidden lg:flex lg:w-[40%] sticky top-0 h-screen overflow-hidden">
        <AmbientNetwork className="w-full h-full" />
      </div>
    </div>
  );
}

function deriveErrorMessage(error: ApiError | null): ResendErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers. Check your connection." };
  }

  // Backend always returns 202 regardless of whether the email is
  // registered/verified/rate-limited (anti-enumeration) — any error here is
  // infrastructure/validation, never "this email doesn't exist".
  return { title: 'Something went wrong. Please try again.' };
}
