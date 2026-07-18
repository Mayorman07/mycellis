import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { changeEmail } from '../lib/api/auth';
import type { ApiError } from '../lib/api/client';
import { INPUT_CLASSES, LABEL_CLASSES } from '../lib/formClasses';

type ChangeEmailErrorMessage = { title: string };

export default function ChangeEmailPage() {
  const navigate = useNavigate();

  const [newEmail, setNewEmail] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const changeMutation = useMutation<
    Awaited<ReturnType<typeof changeEmail>>,
    ApiError,
    Parameters<typeof changeEmail>[0]
  >({
    mutationFn: changeEmail,
  });

  const errorMessage = deriveErrorMessage(changeMutation.error);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    changeMutation.mutate({ newEmail, currentPassword });
  }

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-6xl mx-auto px-6 py-12">
        <div className="max-w-[500px] mx-auto">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-4">
            MYCELLIS · SETTINGS · CHANGE EMAIL
          </p>
          <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-3">
            Change your email.
          </h1>

          {changeMutation.isSuccess ? (
            <>
              <p className="text-ink-muted leading-[1.5] mb-8">
                We sent a verification link to <span className="text-ink">{newEmail}</span>. Click
                it to complete the change. Until then, keep signing in with your current email.
              </p>
              <Link to="/settings" className="text-sm text-ink hover:underline">
                Back to settings
              </Link>
            </>
          ) : (
            <>
              <p className="text-ink-muted mb-10">
                Enter a new email and confirm with your current password. We'll send a
                verification link to the new address.
              </p>

              {errorMessage && (
                <div
                  role="alert"
                  className="mb-6 rounded-md border border-hairline bg-surface-raised px-4 py-3"
                >
                  <div className="flex items-start gap-2">
                    <span className="mt-1.5 w-1.5 h-1.5 rounded-full bg-state-down flex-shrink-0" />
                    <p className="text-sm text-ink">{errorMessage.title}</p>
                  </div>
                </div>
              )}

              <form onSubmit={handleSubmit} className="flex flex-col gap-6">
                <div>
                  <label htmlFor="newEmail" className={LABEL_CLASSES}>
                    New email
                  </label>
                  <input
                    id="newEmail"
                    type="email"
                    required
                    autoComplete="email"
                    inputMode="email"
                    value={newEmail}
                    onChange={(event) => setNewEmail(event.target.value)}
                    placeholder="you@company.com"
                    className={INPUT_CLASSES}
                  />
                </div>

                <div>
                  <label htmlFor="currentPassword" className={LABEL_CLASSES}>
                    Current password
                  </label>
                  <div className="relative">
                    <input
                      id="currentPassword"
                      type={showPassword ? 'text' : 'password'}
                      required
                      autoComplete="current-password"
                      value={currentPassword}
                      onChange={(event) => setCurrentPassword(event.target.value)}
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

                <div className="flex items-center gap-4">
                  <button
                    type="button"
                    onClick={() => navigate('/settings')}
                    className="rounded-md border border-hairline-strong bg-surface-raised px-6 py-4 text-sm font-medium text-ink hover:bg-surface-sunken"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={changeMutation.isPending}
                    aria-busy={changeMutation.isPending}
                    className={`flex-1 rounded-md bg-brand px-4 py-4 text-sm font-medium text-brand-fg hover:bg-brand-hover disabled:opacity-60 ${
                      changeMutation.isPending ? 'cursor-default' : 'cursor-pointer'
                    }`}
                  >
                    {changeMutation.isPending ? 'Updating…' : 'Update email'}
                  </button>
                </div>
              </form>
            </>
          )}
        </div>
      </main>
    </div>
  );
}

function deriveErrorMessage(error: ApiError | null): ChangeEmailErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers." };
  }

  if (error.status === 401) {
    return { title: 'Current password is incorrect.' };
  }

  if (error.status === 409) {
    return { title: "That email's already registered." };
  }

  if (error.status === 400) {
    return { title: error.detail || 'Please check your inputs and try again.' };
  }

  return { title: 'Something went wrong. Please try again.' };
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
