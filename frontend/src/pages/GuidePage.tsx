import { useEffect, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { getTheme, setTheme } from '../lib/theme';

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
        <section className="mb-16">
          <h1 className="font-display font-normal text-[46px] leading-tight tracking-tight text-ink mb-4">
            Welcome to Mycellis
          </h1>
          <p className="text-lg text-ink-muted leading-[1.6] mb-6">
            Your digital ecosystem, breathing in real time.
          </p>
          <p className="text-ink-muted leading-[1.6] mb-4">
            Mycellis watches the endpoints your product depends on and keeps track of their
            health, latency, and uptime.
          </p>
          <p className="text-ink-muted leading-[1.6]">
            When everything is healthy, you know. When something starts to fail, you know before
            your users do.
          </p>
        </section>

        <section className="mb-16">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            The basics
          </p>
          <h2 className="font-display font-normal text-[32px] leading-tight tracking-tight text-ink mb-4">
            What is a stalk?
          </h2>
          <p className="text-ink-muted leading-[1.6] mb-4">
            A stalk is an endpoint you want Mycellis to watch.
          </p>
          <p className="text-ink-muted leading-[1.6] mb-4">
            It can be an API, webhook, service, or any URL your product depends on. Mycellis
            sends a pulse at the interval you choose and records how it responds.
          </p>
          <p className="text-ink-muted leading-[1.6]">
            Think of each stalk as a living part of your digital ecosystem. Healthy stalks
            breathe normally. Stressed stalks tell you something needs attention.
          </p>
        </section>

        <section className="mb-16">
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

        <section className="mb-16">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            Your dashboard
          </p>
          <h2 className="font-display font-normal text-[32px] leading-tight tracking-tight text-ink mb-4">
            Reading your ecosystem
          </h2>
          <p className="text-ink-muted leading-[1.6] mb-8">
            Your dashboard gives you the state of your entire ecosystem at a glance.
          </p>

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

        <section className="mb-16">
          <h2 className="font-display font-normal text-[32px] leading-tight tracking-tight text-ink mb-4">
            Then, let Mycellis watch
          </h2>
          <p className="text-ink-muted leading-[1.6] mb-4">
            Your stalks become more useful with time.
          </p>
          <p className="text-ink-muted leading-[1.6] mb-4">
            As Mycellis collects pulses, you'll build a history of how your ecosystem behaves —
            its uptime, latency, failures, and recovery.
          </p>
          <p className="text-ink-muted leading-[1.6]">
            When everything is healthy, you don't need to stare at a dashboard. When something
            changes, Mycellis tells you.
          </p>
        </section>

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
