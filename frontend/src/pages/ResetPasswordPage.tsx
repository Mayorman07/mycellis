import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { resetPassword } from '../lib/api/auth';
import type { ApiError } from '../lib/api/client';
import { getApiErrorMessage } from '../lib/apiErrorMessage';
import { getTheme, setTheme } from '../lib/theme';
import { AmbientNetwork } from '../components/auth/AmbientNetwork';
import { HINT_CLASSES, INPUT_CLASSES, LABEL_CLASSES } from '../lib/formClasses';

type ResetPasswordErrorMessage = {
  title: string;
  action?: string;
  actionHref?: string;
};

// Backend's ResetPasswordRequest requires @Size(min = 8, max = 64) — same
// minimum as signup.
const PASSWORD_MIN_LENGTH = 8;

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [newPassword, setNewPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // Same locked-cream, per-page mount/unmount pattern as the other auth pages.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  const resetMutation = useMutation<
    Awaited<ReturnType<typeof resetPassword>>,
    ApiError,
    Parameters<typeof resetPassword>[0]
  >({
    mutationFn: resetPassword,
    onSuccess: () => {
      // Reset never creates a session — user must sign in explicitly with
      // the new password.
      navigate('/login', {
        state: { flash: 'Password reset. Sign in with your new password.' },
        replace: true,
      });
    },
  });

  const errorMessage = deriveErrorMessage(resetMutation.error);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }
    resetMutation.mutate({ token, newPassword });
  }

  const passwordHint =
    newPassword.length < PASSWORD_MIN_LENGTH
      ? `${PASSWORD_MIN_LENGTH}+ characters`
      : 'Looking good.';

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
            Set a new password.
          </h1>

          {token ? (
            <>
              <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-8">
                Choose a strong one you'll remember.
              </p>

              {errorMessage && (
                <div
                  role="alert"
                  className="mb-6 rounded-md border border-hairline border-l-4 border-l-state-down bg-surface-raised px-4 py-3"
                >
                  <div className="flex items-start gap-2">
                    <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-state-down flex-shrink-0" />
                    <p className="text-sm text-ink">
                      {errorMessage.title}
                      {errorMessage.action && errorMessage.actionHref && (
                        <>
                          {' '}
                          <Link to={errorMessage.actionHref} className="underline">
                            {errorMessage.action}
                          </Link>
                        </>
                      )}
                    </p>
                  </div>
                </div>
              )}

              <form onSubmit={handleSubmit}>
                <div className="mb-8">
                  <label htmlFor="newPassword" className={LABEL_CLASSES}>
                    New password
                  </label>
                  <div className="relative">
                    <input
                      id="newPassword"
                      type={showPassword ? 'text' : 'password'}
                      required
                      minLength={PASSWORD_MIN_LENGTH}
                      autoComplete="new-password"
                      value={newPassword}
                      onChange={(event) => setNewPassword(event.target.value)}
                      placeholder="••••••••"
                      className={`${INPUT_CLASSES} pr-12`}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword((current) => !current)}
                      aria-label={showPassword ? 'Hide password' : 'Show password'}
                      aria-pressed={showPassword}
                      className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-ink-muted hover:text-ink"
                    >
                      {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                    </button>
                  </div>
                  <p aria-live="polite" className={HINT_CLASSES}>
                    {passwordHint}
                  </p>
                </div>

                <button
                  type="submit"
                  disabled={resetMutation.isPending}
                  aria-busy={resetMutation.isPending}
                  className="w-full rounded-md bg-brand px-4 py-3 text-sm font-medium text-brand-fg disabled:opacity-60"
                >
                  {resetMutation.isPending ? 'Resetting…' : 'Reset password'}
                </button>
              </form>

              <div className="mt-6 text-sm text-ink-muted">
                Changed your mind?{' '}
                <Link to="/login" className="text-ink hover:underline">
                  Sign in
                </Link>
              </div>
            </>
          ) : (
            <div
              role="alert"
              className="rounded-md border border-hairline border-l-4 border-l-state-down bg-surface-raised px-4 py-3"
            >
              <div className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-state-down flex-shrink-0" />
                <p className="text-sm text-ink">
                  This reset link is invalid.{' '}
                  <Link to="/forgot-password" className="underline">
                    Request a new reset link
                  </Link>
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="hidden lg:flex lg:w-[40%] sticky top-0 h-screen overflow-hidden">
        <AmbientNetwork className="w-full h-full" />
      </div>
    </div>
  );
}

function deriveErrorMessage(error: ApiError | null): ResetPasswordErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers." };
  }

  // 404 = token doesn't exist or was already used (cleared to null on use,
  // so a repeat submission looks identical to an invalid token to the
  // backend). 401 = token exists but past its expiry (ExpiredTokenException).
  // Same user-facing outcome either way.
  if (error.status === 404 || error.status === 401) {
    return {
      title: 'This reset link has expired or been used already.',
      action: 'Request a new reset link',
      actionHref: '/forgot-password',
    };
  }

  if (error.status === 429) {
    return { title: 'Too many attempts. Try again in a few minutes.' };
  }

  if (error.status === 400) {
    return { title: getApiErrorMessage(error, 'Please check your password and try again.') };
  }

  return { title: getApiErrorMessage(error) };
}

function EyeIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path
        d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7Z"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="12" cy="12" r="3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.5">
      <path
        d="M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 7 11 7a18.5 18.5 0 0 1-2.16 3.19M6.61 6.61C3.06 8.72 1 12 1 12s4 7 11 7a10.9 10.9 0 0 0 5.39-1.61M14.12 14.12a3 3 0 1 1-4.24-4.24"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M1 1l22 22" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
