type EmptyDashboardProps = {
  orgName: string;
  onCreateStalk: () => void;
};

export function EmptyDashboard({ orgName, onCreateStalk }: EmptyDashboardProps) {
  return (
    <div className="flex flex-col items-center text-center py-24 px-6">
      <div className="relative w-[200px] h-[200px] mb-8 flex items-center justify-center">
        <div className="absolute inset-0 rounded-full bg-brand opacity-10" />
        <div
          className="relative w-[120px] h-[120px] rounded-full bg-brand"
          style={{ animation: 'mycellis-breath-calm 4s ease-in-out infinite' }}
        />
      </div>

      <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-4">
        Welcome to {orgName}
      </p>

      <h1 className="font-display text-[52px] leading-tight tracking-tight text-ink mb-6">
        Nothing is being watched — <em className="italic">yet</em>.
      </h1>

      <p className="max-w-[500px] text-ink-muted leading-relaxed mb-8">
        Mycellis listens to a single endpoint and tells you how it's breathing — its health,
        its latency, the quiet moments and the failures. Plant your first stalk and we'll
        start listening within seconds.
      </p>

      <div className="flex items-center gap-6">
        <button
          type="button"
          onClick={onCreateStalk}
          className="rounded-md bg-brand px-6 py-3 text-sm font-medium text-brand-fg"
        >
          + Plant your first stalk
        </button>
        {/* TODO: link to the 2-minute guide once that page exists */}
        <button type="button" className="text-sm text-ink-subtle">
          Read the 2-minute guide →
        </button>
      </div>
    </div>
  );
}
