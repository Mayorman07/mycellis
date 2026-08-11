import { useEffect, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { forgotPassword } from '../lib/api/auth';
import type { ApiError } from '../lib/api/client';
import { getTheme, setTheme } from '../lib/theme';
import { AmbientNetwork } from '../components/auth/AmbientNetwork';
import { HINT_CLASSES, INPUT_CLASSES, INPUT_ERROR_STYLE, LABEL_CLASSES } from '../lib/formClasses';
import { isValidEmail } from '../lib/validation';

type ForgotPasswordErrorMessage = { title: string };

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [emailTouched, setEmailTouched] = useState(false);

  // Same locked-cream, per-page mount/unmount pattern as the other auth pages.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  const forgotMutation = useMutation<
    Awaited<ReturnType<typeof forgotPassword>>,
    ApiError,
    Parameters<typeof forgotPassword>[0]
  >({
    mutationFn: forgotPassword,
  });

  const errorMessage = deriveErrorMessage(forgotMutation.error);
  const emailIsValid = isValidEmail(email);
  const showEmailError = emailTouched && email !== '' && !emailIsValid;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    forgotMutation.mutate({ email });
  }

  function handleTryAgain() {
    forgotMutation.reset();
    setEmail('');
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
            Forgot your password?
          </h1>

          {forgotMutation.isSuccess ? (
            <>
              <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-6">
                If an account exists for that email, we sent a reset link. Check your inbox and
                spam folder.
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
                Enter your email and we'll send a reset link.
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
                    onBlur={() => setEmailTouched(true)}
                    placeholder="you@company.com"
                    aria-invalid={showEmailError}
                    className={INPUT_CLASSES}
                    style={showEmailError ? INPUT_ERROR_STYLE : undefined}
                  />
                  {showEmailError && (
                    <p className={`${HINT_CLASSES} text-state-down`}>
                      Please enter a valid email address
                    </p>
                  )}
                </div>

                <button
                  type="submit"
                  disabled={forgotMutation.isPending || !emailIsValid}
                  aria-busy={forgotMutation.isPending}
                  className="w-full rounded-md bg-brand px-4 py-3 text-sm font-medium text-brand-fg disabled:opacity-60"
                >
                  {forgotMutation.isPending ? 'Sending…' : 'Send reset link'}
                </button>
              </form>

              <div className="mt-6 text-sm text-ink-muted">
                Remember your password?{' '}
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

function deriveErrorMessage(error: ApiError | null): ForgotPasswordErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers. Check your connection." };
  }

  // Backend always returns 202 regardless of whether the account exists
  // (anti-enumeration) — any error here is infrastructure/validation, never
  // "this email doesn't exist", so a single generic message covers it.
  return { title: 'Something went wrong. Please try again.' };
}
