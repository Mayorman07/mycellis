import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { login } from '../lib/api/auth';
import type { ApiError } from '../lib/api/client';
import { getTheme, setTheme } from '../lib/theme';
import { AmbientNetwork } from '../components/auth/AmbientNetwork';

type LocationState = { from?: string } | null;

type LoginErrorMessage = {
  title: string;
  detail?: string;
  action?: string;
  actionHref?: string;
};

const INPUT_CLASSES =
  'w-full rounded-md border-[1.5px] border-[color-mix(in_srgb,var(--color-ink)_25%,transparent)] bg-surface-sunken px-4 py-4 text-base tracking-tight text-ink placeholder:text-ink-subtle shadow-[inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent)] transition-all duration-[250ms] ease-in-out focus:outline-none focus:border-brand focus:[box-shadow:inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent),0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]';

export default function LoginPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const state = location.state as LocationState;
  const from = state?.from ?? '/dashboard';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // Auth pages read as a calm, consistent front door regardless of the
  // visitor's dashboard theme preference — locked to cream while mounted,
  // restored the instant they navigate away. Per-page, not global: each
  // auth page owns this decision independently.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  const loginMutation = useMutation<
    Awaited<ReturnType<typeof login>>,
    ApiError,
    Parameters<typeof login>[0]
  >({
    mutationFn: login,
    onSuccess: () => {
      // Session context is stale until this refetches — must happen before
      // navigating, or the destination route's ProtectedRoute check would
      // still see the old (unauthenticated) session state.
      queryClient.invalidateQueries({ queryKey: ['me'] });
      navigate(from, { replace: true });
    },
  });

  const errorMessage = deriveErrorMessage(loginMutation.error, email);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    loginMutation.mutate({ email, password, rememberMe });
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

          <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-3">
            Welcome back.
          </h1>
          <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-8">
            Your systems are waiting. Monitor every endpoint from one place.
          </p>

          {errorMessage && (
            <div
              role="alert"
              className="mb-6 rounded-md border border-hairline border-l-4 border-l-state-down bg-surface-raised px-4 py-3"
            >
              <div className="flex items-start gap-2">
                <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-state-down flex-shrink-0" />
                <div>
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
                  {errorMessage.detail && (
                    <p className="text-xs text-ink-subtle mt-1">{errorMessage.detail}</p>
                  )}
                </div>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="mb-4">
              <label
                htmlFor="email"
                className="block font-mono uppercase text-xs tracking-wider text-ink-subtle mb-2"
              >
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

            <div className="mb-6">
              <label
                htmlFor="password"
                className="block font-mono uppercase text-xs tracking-wider text-ink-subtle mb-2"
              >
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
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
            </div>

            <label className="flex items-start gap-3 mb-6 cursor-pointer">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(event) => setRememberMe(event.target.checked)}
                className="peer sr-only"
              />
              <span
                aria-hidden="true"
                className={`mt-0.5 flex-shrink-0 w-[18px] h-[18px] rounded border flex items-center justify-center transition-colors duration-150 ease-in-out peer-focus-visible:[box-shadow:0_0_0_3px_color-mix(in_srgb,var(--color-brand)_15%,transparent)] ${
                  rememberMe ? 'bg-brand border-brand' : 'bg-surface-sunken border-hairline'
                }`}
              >
                {rememberMe && (
                  <svg
                    viewBox="0 0 10 10"
                    width="10"
                    height="10"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.75"
                    className="text-brand-fg"
                  >
                    <path d="M1.5 5.2l2.4 2.4 4.6-5.2" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                )}
              </span>
              <span>
                <span className="block text-sm text-ink">Trust this device for 30 days</span>
                <span className="block text-xs text-ink-subtle">Stays logged in on this device.</span>
              </span>
            </label>

            <button
              type="submit"
              disabled={loginMutation.isPending}
              className="w-full rounded-md bg-brand px-4 py-3 text-sm font-medium text-brand-fg disabled:opacity-60"
            >
              {loginMutation.isPending ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <div className="flex items-center justify-between mt-6 text-sm text-ink-muted">
            <Link to="/signup" className="hover:text-ink">
              Create account
            </Link>
            <Link to="/forgot-password" className="hover:text-ink">
              Forgot password?
            </Link>
          </div>
        </div>
      </div>

      <div className="hidden lg:flex lg:w-[40%] overflow-hidden">
        <AmbientNetwork className="w-full h-full" />
      </div>
    </div>
  );
}

function deriveErrorMessage(error: ApiError | null, email: string): LoginErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers. Check your connection and try again." };
  }

  if (error.status === 403 && error.type?.includes('account-not-verified')) {
    return {
      title: 'Verify your email before signing in.',
      action: 'Resend verification email',
      actionHref: `/resend-verification?email=${encodeURIComponent(email)}`,
    };
  }

  if (error.status === 429) {
    return { title: 'Too many attempts. Try again in a few minutes.' };
  }

  if (error.status === 400 || error.status === 401) {
    return { title: 'Email or password is incorrect. Try again.' };
  }

  return {
    title: 'Something went wrong. Please try again.',
    detail: error.title || undefined,
  };
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
