type ErrorDashboardProps = {
  error: Error | null;
  onRetry: () => void;
};

const MAX_ERROR_MESSAGE_LENGTH = 100;

export function ErrorDashboard({ error, onRetry }: ErrorDashboardProps) {
  const truncatedMessage =
    error && error.message.length > MAX_ERROR_MESSAGE_LENGTH
      ? `${error.message.slice(0, MAX_ERROR_MESSAGE_LENGTH)}…`
      : error?.message;

  return (
    <div className="flex items-center justify-center py-24 px-6">
      <div className="w-full max-w-[400px] rounded-md border border-hairline bg-surface-raised p-8 text-center">
        <AlertIcon />
        <h1 className="font-display text-[24px] text-ink mt-4 mb-2">Couldn&apos;t reach your stalks</h1>
        <p className="text-sm text-ink-muted leading-relaxed mb-6">
          Something went wrong loading your monitoring data. If this keeps happening, check
          your connection or refresh the page.
        </p>
        <button
          type="button"
          onClick={onRetry}
          className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-brand-fg"
        >
          Try again
        </button>
        {truncatedMessage && (
          <p className="font-mono text-[11px] text-ink-subtle mt-4 truncate">{truncatedMessage}</p>
        )}
      </div>
    </div>
  );
}

function AlertIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="32"
      height="32"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      className="text-state-down mx-auto"
    >
      <path
        d="M12 9v4m0 4h.01M10.29 3.86 1.82 18a1 1 0 0 0 .86 1.5h18.64a1 1 0 0 0 .86-1.5L13.71 3.86a1 1 0 0 0-1.72 0Z"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
