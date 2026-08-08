import { useEffect, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { getTheme, setTheme } from '../lib/theme';

// Same petal geometry as MycellisFlowerLoader — copied, not imported, since
// this ornament is a static section divider (no animation, no motion prefs).
const DIVIDER_PETAL_PATH = 'M 50,47 C 59,42 59,26 55,17 C 53,12.5 47,12.5 45,17 C 41,26 41,42 50,47 Z';
const DIVIDER_PETAL_ANGLES = [0, 72, 144, 216, 288];
const DIVIDER_COLOR = 'color-mix(in srgb, var(--color-brand) 35%, transparent)';

export default function GuidePage() {
  // Same locked-cream, per-page mount/unmount pattern as the auth pages and
  // the public status page — this is a public marketing/docs surface,
  // session-agnostic, so it ignores whatever theme a logged-in visitor last
  // set for themselves and restores it the instant they navigate away.
  useEffect(() => {
    const previousTheme = getTheme();
    setTheme('cream');
    return () => {
      setTheme(previousTheme);
    };
  }, []);

  return (
    <div className="min-h-screen bg-surface">
      <header className="border-b border-hairline bg-surface px-6 py-4">
        <div className="max-w-2xl mx-auto">
          <Link to="/" className="font-display text-xl text-ink tracking-tight">
            MYCELLIS
          </Link>
        </div>
      </header>

      <main className="max-w-2xl mx-auto px-6 py-16">
        <section>
          <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-4">
            Welcome to Mycellis
          </h1>
          <p className="text-lg text-ink-muted leading-[1.6] mb-6">
            Your digital ecosystem, breathing in real time.
          </p>
          <p className="text-ink-muted leading-[1.6] mb-4 overflow-hidden [&::first-letter]:font-display [&::first-letter]:text-ink [&::first-letter]:float-left [&::first-letter]:leading-none [&::first-letter]:mr-2 [&::first-letter]:mt-1 [&::first-letter]:text-[56px] md:[&::first-letter]:text-[72px]">
            Mycellis watches the endpoints your product depends on and keeps track of their
            health, latency, and uptime.
          </p>
          <PullQuote>
            When everything is healthy, you know. When something starts to fail, you know before
            your users do.
          </PullQuote>
          <p className="text-ink-muted leading-[1.6]">
            When everything is healthy, you know. When something starts to fail, you know before
            your users do.
          </p>
        </section>

        <SectionDivider />

        <section>
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            The basics
          </p>
          <h2 className="font-display font-normal text-[32px] leading-tight tracking-tight text-ink mb-4">
            What is a stalk?
          </h2>
          <p className="text-ink-muted leading-[1.6] mb-4">
            A stalk is an endpoint you want Mycellis to watch.
          </p>
          <PullQuote>Think of each stalk as a living part of your digital ecosystem.</PullQuote>
          <p className="text-ink-muted leading-[1.6] mb-4">
            It can be an API, webhook, service, or any URL your product depends on. Mycellis
            sends a pulse at the interval you choose and records how it responds.
          </p>
          <p className="text-ink-muted leading-[1.6]">
            Think of each stalk as a living part of your digital ecosystem. Healthy stalks
            breathe normally. Stressed stalks tell you something needs attention.
          </p>
        </section>

        <SectionDivider />

        <section>
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            Getting started
          </p>
          <h2 className="font-display font-normal text-[32px] leading-tight tracking-tight text-ink mb-4">
            Plant your first stalk
          </h2>
          <p className="text-ink-muted leading-[1.6] mb-8">You only need a few seconds.</p>

          <div className="space-y-8">
            <GuideStep
              number="01"
              title="Name it"
              body="Give your endpoint a name you'll recognize."
              example="Stripe API"
            />
            <GuideStep
              number="02"
              title="Give it a URL"
              body="Paste the endpoint you want Mycellis to watch."
              example="https://api.example.com/health"
            />
            <GuideStep
              number="03"
              title="Choose its rhythm"
              body="Tell Mycellis how often to check it. Every 60 seconds is a good place to start."
            />
            <GuideStep
              number="04"
              title="Plant it"
              body="Save your stalk. That's it. Mycellis starts pulsing immediately."
            />
          </div>
        </section>

        <SectionDivider />

        <section>
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            Your dashboard
          </p>
          <h2 className="font-display font-normal text-[32px] leading-tight tracking-tight text-ink mb-4">
            Reading your ecosystem
          </h2>
          <p className="text-ink-muted leading-[1.6] mb-8">
            Your dashboard gives you the state of your entire ecosystem at a glance.
          </p>
          <PullQuote>
            Your dashboard gives you the state of your entire ecosystem at a glance.
          </PullQuote>

          <div className="space-y-5">
            <MetricExplainer
              term="Ecosystem uptime"
              body="The percentage of successful pulses across all your stalks. Higher is healthier."
            />
            <MetricExplainer
              term="Stalks"
              body="The number of endpoints you're currently monitoring."
            />
            <MetricExplainer
              term="Stressed"
              body="The number of stalks that aren't breathing normally right now. These deserve your attention."
            />
            <MetricExplainer
              term="Incidents"
              body="Problems that have been detected and are being tracked."
            />
          </div>
        </section>

        <SectionDivider />

        <section>
          <h2 className="font-display font-normal text-[32px] leading-tight tracking-tight text-ink mb-4">
            Then, let Mycellis watch
          </h2>
          <p className="text-ink-muted leading-[1.6] mb-4">
            Your stalks become more useful with time.
          </p>
          <PullQuote>When something changes, Mycellis tells you.</PullQuote>
          <p className="text-ink-muted leading-[1.6] mb-4">
            As Mycellis collects pulses, you'll build a history of how your ecosystem behaves —
            its uptime, latency, failures, and recovery.
          </p>
          <p className="text-ink-muted leading-[1.6]">
            When everything is healthy, you don't need to stare at a dashboard. When something
            changes, Mycellis tells you.
          </p>
        </section>

        <SectionDivider />

        <section className="mb-16">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">FAQ</p>
          <h2 className="font-display font-normal text-[32px] leading-tight tracking-tight text-ink mb-6">
            Frequently asked questions
          </h2>

          <div className="space-y-6">
            <FaqItem question="How much does Mycellis cost?" answer="Mycellis is free during beta." />
            <FaqItem
              question="Can I share a status page publicly?"
              answer="Yes. Your public status page lets you communicate the health of your ecosystem without requiring visitors to log in."
            />
            <FaqItem question="Can Mycellis monitor authenticated endpoints?" answer="Coming soon." />
          </div>
        </section>

        <section className="pt-8 border-t border-hairline text-center">
          <p className="text-ink-muted mb-4">Ready to plant your first stalk?</p>
          <Link to="/login" className="text-ink font-medium hover:underline">
            Sign in →
          </Link>
        </section>
      </main>
    </div>
  );
}

function GuideStep({
  number,
  title,
  body,
  example,
}: {
  number: string;
  title: string;
  body: ReactNode;
  example?: string;
}) {
  return (
    <div>
      <div className="flex items-baseline gap-3 mb-2">
        <span className="font-mono font-bold text-base text-brand">{number}</span>
        <h3 className="font-display font-normal text-[22px] leading-tight text-ink">{title}</h3>
      </div>
      <p className="text-ink-muted leading-[1.6] mb-3">{body}</p>
      {example && (
        <div className="inline-block bg-surface-raised rounded px-3 py-2 font-mono text-sm text-ink-muted">
          Example: <span className="text-ink">{example}</span>
        </div>
      )}
    </div>
  );
}

function MetricExplainer({ term, body }: { term: string; body: ReactNode }) {
  return (
    <p className="text-ink-muted leading-[1.6]">
      <span className="font-semibold text-ink">{term}</span> — {body}
    </p>
  );
}

function FaqItem({ question, answer }: { question: string; answer: ReactNode }) {
  return (
    <div>
      <p className="font-semibold text-ink mb-1">{question}</p>
      <p className="text-ink-muted leading-[1.6]">{answer}</p>
    </div>
  );
}

function PullQuote({ children }: { children: ReactNode }) {
  return (
    <blockquote className="font-display italic text-ink-subtle text-[20px] md:text-[22px] leading-snug border-l-2 border-brand pl-4 my-6 md:my-8">
      {children}
    </blockquote>
  );
}

function SectionDivider() {
  return (
    <div role="presentation" aria-hidden="true" className="flex justify-center my-12 md:my-16">
      <svg viewBox="0 0 100 100" className="w-5 h-5 md:w-6 md:h-6">
        {DIVIDER_PETAL_ANGLES.map((angle) => (
          <g key={angle} transform={`rotate(${angle} 50 50)`}>
            <path d={DIVIDER_PETAL_PATH} fill={DIVIDER_COLOR} />
          </g>
        ))}
        <circle cx="50" cy="50" r="8" fill={DIVIDER_COLOR} />
      </svg>
    </div>
  );
}
