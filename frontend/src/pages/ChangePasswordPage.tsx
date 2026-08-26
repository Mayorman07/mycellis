import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { changePassword } from '../lib/api/auth';
import type { ApiError } from '../lib/api/client';
import { getApiErrorMessage } from '../lib/apiErrorMessage';
import { HINT_CLASSES, INPUT_CLASSES, LABEL_CLASSES } from '../lib/formClasses';

type ChangePasswordErrorMessage = { title: string };

const PASSWORD_MIN_LENGTH = 8;

export default function ChangePasswordPage() {
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const changeMutation = useMutation<
    Awaited<ReturnType<typeof changePassword>>,
    ApiError,
    Parameters<typeof changePassword>[0]
  >({
    mutationFn: changePassword,
    onSuccess: () => {
      // Session stays valid — the backend doesn't invalidate it on a password
      // change, so there's no reason to force the user back through login.
      navigate('/settings', { state: { flash: 'Password updated.' }, replace: true });
    },
  });

  const errorMessage = deriveErrorMessage(changeMutation.error);

  const newPasswordHint =
    newPassword.length < PASSWORD_MIN_LENGTH ? `${PASSWORD_MIN_LENGTH}+ characters` : 'Looking good.';
  const confirmMismatch = confirmPassword.length > 0 && newPassword !== confirmPassword;

  const canSubmit =
    currentPassword.length > 0 &&
    newPassword.length >= PASSWORD_MIN_LENGTH &&
    confirmPassword.length > 0 &&
    newPassword === confirmPassword;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!canSubmit) {
      return;
    }
    changeMutation.mutate({ currentPassword, newPassword });
  }

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-6xl mx-auto px-6 py-12">
        <div className="max-w-[500px] mx-auto">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-4">
            MYCELLIS · SETTINGS · CHANGE PASSWORD
          </p>
          <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-3">
            Change your password.
          </h1>
          <p className="text-ink-muted mb-10">
            Enter your current password and choose a new one.
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
              <label htmlFor="currentPassword" className={LABEL_CLASSES}>
                Current password
              </label>
              <div className="relative">
                <input
                  id="currentPassword"
                  type={showCurrent ? 'text' : 'password'}
                  required
                  autoComplete="current-password"
                  value={currentPassword}
                  onChange={(event) => setCurrentPassword(event.target.value)}
                  placeholder="••••••••"
                  className={`${INPUT_CLASSES} pr-12`}
                />
                <button
                  type="button"
                  onClick={() => setShowCurrent((current) => !current)}
                  aria-label={showCurrent ? 'Hide password' : 'Show password'}
                  aria-pressed={showCurrent}
                  className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-ink-muted hover:text-ink"
                >
                  {showCurrent ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
            </div>

            <div>
              <label htmlFor="newPassword" className={LABEL_CLASSES}>
                New password
              </label>
              <div className="relative">
                <input
                  id="newPassword"
                  type={showNew ? 'text' : 'password'}
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
                  onClick={() => setShowNew((current) => !current)}
                  aria-label={showNew ? 'Hide password' : 'Show password'}
                  aria-pressed={showNew}
                  className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-ink-muted hover:text-ink"
                >
                  {showNew ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
              <p aria-live="polite" className={HINT_CLASSES}>
                {newPasswordHint}
              </p>
            </div>

            <div>
              <label htmlFor="confirmPassword" className={LABEL_CLASSES}>
                Confirm new password
              </label>
              <div className="relative">
                <input
                  id="confirmPassword"
                  type={showConfirm ? 'text' : 'password'}
                  required
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  placeholder="••••••••"
                  className={`${INPUT_CLASSES} pr-12`}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirm((current) => !current)}
                  aria-label={showConfirm ? 'Hide password' : 'Show password'}
                  aria-pressed={showConfirm}
                  className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-ink-muted hover:text-ink"
                >
                  {showConfirm ? <EyeOffIcon /> : <EyeIcon />}
                </button>
              </div>
              {confirmMismatch && (
                <p aria-live="polite" className={`${HINT_CLASSES} text-state-down`}>
                  Doesn't match
                </p>
              )}
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
                disabled={!canSubmit || changeMutation.isPending}
                aria-busy={changeMutation.isPending}
                className={`flex-1 rounded-md bg-brand px-4 py-4 text-sm font-medium text-brand-fg hover:bg-brand-hover disabled:opacity-60 ${
                  changeMutation.isPending ? 'cursor-default' : 'cursor-pointer'
                }`}
              >
                {changeMutation.isPending ? 'Updating…' : 'Update password'}
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}

function deriveErrorMessage(error: ApiError | null): ChangePasswordErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers." };
  }

  if (error.status === 401) {
    return { title: 'Current password is incorrect.' };
  }

  // AuthServiceImpl throws ConflictException (409) for "new password matches
  // current" — not a 400 as it might seem at a glance (this is the same
  // exception class/status used for duplicate-email conflicts elsewhere).
  if (error.status === 409) {
    return { title: 'New password must be different from current.' };
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
