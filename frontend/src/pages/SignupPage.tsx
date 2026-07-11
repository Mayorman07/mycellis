import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { signup } from '../lib/api/auth';
import type { ApiError } from '../lib/api/client';
import type { Gender } from '../lib/types';
import { getTheme, setTheme } from '../lib/theme';
import { AmbientNetwork } from '../components/auth/AmbientNetwork';

type SignupErrorMessage = {
  title: string;
  detail?: string;
  action?: string;
  actionHref?: string;
};

const INPUT_CLASSES =
  'w-full rounded-md border-[1.5px] border-[color-mix(in_srgb,var(--color-ink)_25%,transparent)] bg-surface-sunken px-4 py-4 text-base tracking-tight text-ink placeholder:text-ink-subtle shadow-[inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent)] transition-all duration-[250ms] ease-in-out focus:outline-none focus:border-brand focus:[box-shadow:inset_0_1px_2px_color-mix(in_srgb,var(--color-ink)_4%,transparent),0_0_0_4px_color-mix(in_srgb,var(--color-brand)_20%,transparent)]';

const LABEL_CLASSES = 'block font-mono uppercase text-xs tracking-wider text-ink-subtle mb-2';

// Backend's CreateUserRequest requires @Size(min = 8, max = 64) — not the 6
// characters originally assumed for the strength hint. Using the real
// minimum here so the hint never tells the user "looking good" on a length
// the backend then rejects.
const PASSWORD_MIN_LENGTH = 8;

const MOBILE_NUMBER_PATTERN = '^\\+?[0-9]{11,15}$';

export default function SignupPage() {
  const navigate = useNavigate();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');
  const [gender, setGender] = useState<Gender | ''>('');
  const [organizationName, setOrganizationName] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  // Auth pages read as a calm, consistent front door regardless of the
  // visitor's dashboard theme preference — locked to cream while mounted,
  // restored the instant they navigate away. Per-page, not global: each
  // auth page owns this decision independently (same pattern as LoginPage).
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  const signupMutation = useMutation<
    Awaited<ReturnType<typeof signup>>,
    ApiError,
    Parameters<typeof signup>[0]
  >({
    mutationFn: signup,
    onSuccess: (response) => {
      // Signup never creates a session — only login does. Land on a
      // dedicated "check your email" page rather than the dashboard.
      navigate('/verify-pending', { state: { email: response.email } });
    },
  });

  const errorMessage = deriveErrorMessage(signupMutation.error);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (gender === '') {
      return;
    }
    signupMutation.mutate({
      firstName,
      lastName,
      email,
      mobileNumber,
      gender,
      organizationName,
      password,
    });
  }

  const passwordHint =
    password.length < PASSWORD_MIN_LENGTH
      ? `${PASSWORD_MIN_LENGTH}+ characters`
      : 'Looking good.';

  return (
    <div className="min-h-screen flex bg-surface">
      <div className="w-full lg:w-[60%] relative flex flex-col px-8 sm:px-16 pt-16 pb-16 bg-[radial-gradient(ellipse_at_center,transparent_0%,color-mix(in_srgb,var(--color-ink)_2%,transparent)_100%)]">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-1/2 bg-[linear-gradient(135deg,color-mix(in_srgb,var(--color-brand)_4%,transparent)_0%,transparent_60%)]"
        />

        <div className="relative max-w-md w-full mx-auto lg:mx-0">
          <div className="mb-8">
            <p className="font-mono uppercase tracking-widest text-sm font-medium text-ink">
              MYCELLIS
            </p>
            <p className="font-mono tracking-wider text-xs font-normal text-ink-muted mt-0.5">
              Digital Ecology Monitor
            </p>
          </div>

          <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-3">
            Create your monitor.
          </h1>
          <p className="max-w-[440px] text-ink-muted leading-[1.5] mb-8">
            Start listening to your endpoints in under two minutes.
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
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
              <div>
                <label htmlFor="firstName" className={LABEL_CLASSES}>
                  First name
                </label>
                <input
                  id="firstName"
                  type="text"
                  required
                  autoComplete="given-name"
                  value={firstName}
                  onChange={(event) => setFirstName(event.target.value)}
                  placeholder="Ada"
                  className={INPUT_CLASSES}
                />
              </div>
              <div>
                <label htmlFor="lastName" className={LABEL_CLASSES}>
                  Last name
                </label>
                <input
                  id="lastName"
                  type="text"
                  required
                  autoComplete="family-name"
                  value={lastName}
                  onChange={(event) => setLastName(event.target.value)}
                  placeholder="Lovelace"
                  className={INPUT_CLASSES}
                />
              </div>
            </div>

            <div className="mb-4">
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

            <div className="mb-4">
              <label htmlFor="mobileNumber" className={LABEL_CLASSES}>
                Mobile number
              </label>
              <input
                id="mobileNumber"
                type="tel"
                required
                autoComplete="tel"
                inputMode="tel"
                pattern={MOBILE_NUMBER_PATTERN}
                value={mobileNumber}
                onChange={(event) => setMobileNumber(event.target.value)}
                placeholder="+2348012345678"
                className={INPUT_CLASSES}
              />
            </div>

            <div className="mb-6">
              <label htmlFor="gender" className={LABEL_CLASSES}>
                Gender
              </label>
              <div className="relative">
                <select
                  id="gender"
                  required
                  autoComplete="sex"
                  value={gender}
                  onChange={(event) => setGender(event.target.value as Gender)}
                  className={`${INPUT_CLASSES} appearance-none pr-10 ${
                    gender === '' ? 'text-ink-subtle' : 'text-ink'
                  }`}
                >
                  <option value="" disabled>
                    Select gender
                  </option>
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Non-binary / Other</option>
                  <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
                </select>
                <ChevronIcon className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-ink-subtle" />
              </div>
            </div>

            <div className="mb-6">
              <label htmlFor="organizationName" className={LABEL_CLASSES}>
                Organization name
              </label>
              <input
                id="organizationName"
                type="text"
                required
                autoComplete="organization"
                value={organizationName}
                onChange={(event) => setOrganizationName(event.target.value)}
                placeholder="Acme Inc."
                className={INPUT_CLASSES}
              />
              <p className="mt-2 text-xs text-ink-subtle">This is where your monitoring lives.</p>
            </div>

            <div className="mb-8">
              <label htmlFor="password" className={LABEL_CLASSES}>
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  minLength={PASSWORD_MIN_LENGTH}
                  autoComplete="new-password"
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
              <p aria-live="polite" className="mt-2 text-xs text-ink-subtle">
                {passwordHint}
              </p>
            </div>

            <button
              type="submit"
              disabled={signupMutation.isPending}
              className="w-full rounded-md bg-brand px-4 py-3 text-sm font-medium text-brand-fg disabled:opacity-60"
            >
              {signupMutation.isPending ? 'Creating account…' : 'Create account'}
            </button>
          </form>

          <div className="mt-6 text-sm text-ink-muted">
            Already have an account?{' '}
            <Link to="/login" className="text-ink hover:underline">
              Sign in
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

function deriveErrorMessage(error: ApiError | null): SignupErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers. Check your connection." };
  }

  if (error.status === 409) {
    // Only email uniqueness is enforced today (UserServiceImpl.createUser).
    // Organization name has no backend uniqueness constraint — duplicate
    // names silently get a disambiguated slug instead of being rejected —
    // so this branch is currently unreachable until that changes.
    if (error.detail?.toLowerCase().includes('organization')) {
      return { title: "That workspace name's taken. Try another." };
    }
    return {
      title: "That email's already registered.",
      action: 'Sign in instead',
      actionHref: '/login',
    };
  }

  if (error.status === 429) {
    return { title: 'Too many attempts. Try again in a few minutes.' };
  }

  if (error.status === 400) {
    return { title: error.detail || 'Please check the form and try again.' };
  }

  return {
    title: 'Something went wrong. Please try again.',
    detail: error.title || undefined,
  };
}

function ChevronIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="1.5" className={className}>
      <path d="M6 9l6 6 6-6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
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
