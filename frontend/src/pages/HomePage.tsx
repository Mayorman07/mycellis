export default function HomePage() {
  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-8">
      <div className="max-w-md w-full bg-surface-raised rounded-md border border-hairline p-12">
        <div className="w-10 h-0.5 bg-brand mb-10"></div>
        <h1 className="text-brand text-sm font-extrabold tracking-[3px] uppercase mb-12">
          Mycellis
        </h1>
        <h2 className="text-ink text-3xl font-bold tracking-tight mb-4">
          Watch your services breathe.
        </h2>
        <p className="text-ink-muted leading-relaxed">
          Real-time monitoring with the calm of a living system.
        </p>
      </div>
    </div>
  );
}