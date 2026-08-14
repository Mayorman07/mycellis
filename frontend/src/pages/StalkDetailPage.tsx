import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { deleteStalk, getStalk } from '../lib/api/stalks';
import { getRecentPulses } from '../lib/api/pulses';
import type { ApiError } from '../lib/api/client';
import type { Pulse } from '../lib/types';
import { StatusDot } from '../components/dashboard/StatusDot';
import { StatePill } from '../components/dashboard/StatePill';
import { Sparkline } from '../components/dashboard/Sparkline';
import { DeleteStalkModal } from '../components/stalks/DeleteStalkModal';
import { pluralize } from '../lib/pluralize';

const PULSE_HISTORY_LIMIT = 200;
const REFETCH_INTERVAL_MS = 15_000;
const PULSE_GRID_COLS = 'grid-cols-[90px_120px_40px_80px_80px]';
const SPARKLINE_BAR_WIDTH = 3;
const SPARKLINE_GAP = 1;
// Matches Sparkline's own width formula (slotCount * barWidth + (slotCount - 1) * gap)
// so the OLDEST/NEWEST label row lines up with the rendered bars' actual edges.
const SPARKLINE_WIDTH_PX =
  PULSE_HISTORY_LIMIT * SPARKLINE_BAR_WIDTH + (PULSE_HISTORY_LIMIT - 1) * SPARKLINE_GAP;

function formatRelativeTime(iso: string): string {
  const now = Date.now();
  const then = new Date(iso).getTime();
  const seconds = Math.floor((now - then) / 1000);

  if (seconds < 60) return `${seconds}s ago`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
  return `${Math.floor(seconds / 86400)}d ago`;
}

export default function StalkDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [showDeleteModal, setShowDeleteModal] = useState(false);

  const stalkQuery = useQuery<Awaited<ReturnType<typeof getStalk>>, ApiError>({
    queryKey: ['stalk', id],
    queryFn: () => getStalk(id!),
    enabled: !!id,
    refetchInterval: REFETCH_INTERVAL_MS,
  });

  const deleteMutation = useMutation<void, ApiError, void>({
    mutationFn: () => deleteStalk(id!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stalks'] });
      queryClient.removeQueries({ queryKey: ['stalk', id] });
      navigate('/dashboard');
    },
  });

  const pulsesQuery = useQuery<Pulse[], ApiError>({
    queryKey: ['stalk-pulses', id],
    queryFn: () => getRecentPulses(id!, PULSE_HISTORY_LIMIT),
    enabled: !!id && !!stalkQuery.data,
    refetchInterval: REFETCH_INTERVAL_MS,
  });

  if (stalkQuery.isPending) {
    return (
      <div className="min-h-screen bg-surface">
        <main className="max-w-6xl mx-auto px-6 py-12">
          <div className="flex items-center gap-2 py-10">
            <span
              className="inline-block w-2 h-2 rounded-full bg-state-healthy"
              style={{ animation: 'mycellis-breath 2.6s ease-in-out infinite' }}
            />
            <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">
              Loading stalk…
            </p>
          </div>
        </main>
      </div>
    );
  }

  if (stalkQuery.isError) {
    // getStalkById has no other validation surface on a bare GET — a missing
    // stalk throws IllegalArgumentException (400, type "invalid-parameter"),
    // and a syntactically-invalid id string never reaches the service at all,
    // failing UUID path-variable binding first (400, type "malformed-request").
    // Neither is a 404 — both read as "not found" to the user regardless.
    if (stalkQuery.error.status === 400) {
      return (
        <StatusMessage
          title="Stalk not found."
          detail="It may have been deleted."
        />
      );
    }

    if (stalkQuery.error.status === 403) {
      return (
        <StatusMessage
          title="You don't have access to this stalk."
          detail="If you think this is a mistake, check with your workspace owner."
        />
      );
    }

    return (
      <StatusMessage
        title="Couldn't load this stalk."
        detail="Something went wrong loading this stalk's data."
        onRetry={() => stalkQuery.refetch()}
      />
    );
  }

  const stalk = stalkQuery.data;
  const pulses = pulsesQuery.data ?? [];

  const uptimeLabel = `${stalk.healthIndex.toFixed(1)}%`;
  const cardLatencyLabel = stalk.averageLatencyMs === null ? '—' : `${stalk.averageLatencyMs} ms`;

  const uptimeColorClass =
    stalk.healthIndex >= 99
      ? 'text-state-healthy'
      : stalk.healthIndex >= 95
        ? 'text-state-stressed'
        : 'text-state-down';
  const failuresColorClass = stalk.consecutiveFailures > 0 ? 'text-state-down' : 'text-ink';

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-[1000px] mx-auto px-6 py-12">
        <div className="flex items-center justify-between mb-8">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">
            MYCELLIS · STALK
          </p>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => navigate(`/stalks/${id}/edit`)}
              className="rounded-md border border-hairline bg-surface-raised px-4 py-2 text-sm font-medium text-ink"
            >
              Edit
            </button>
            <button
              type="button"
              onClick={() => setShowDeleteModal(true)}
              className="rounded-md border border-hairline bg-surface-raised px-4 py-2 text-sm font-medium text-ink hover:text-state-down"
            >
              Delete
            </button>
          </div>
        </div>

        <h1 className="font-display font-normal text-[40px] leading-tight tracking-tight text-ink mb-2">
          {stalk.nickname}.
        </h1>
        <p className="font-mono text-sm text-ink-muted mb-8 break-all">{stalk.url}</p>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <MetricCard
            label="Uptime"
            value={uptimeLabel}
            valueColorClass={uptimeColorClass}
            secondary={`last ${PULSE_HISTORY_LIMIT} ${pluralize(PULSE_HISTORY_LIMIT, 'pulse')}`}
          />
          <MetricCard
            label="Avg latency"
            value={cardLatencyLabel}
            valueColorClass="text-ink"
            secondary="median across pulses"
          />
          <MetricCard
            label="Failures"
            value={String(stalk.consecutiveFailures)}
            valueColorClass={failuresColorClass}
            secondary="consecutive"
          />
        </div>

        <div className="flex items-center gap-3 mb-10">
          <StatusDot reliabilityState={stalk.reliabilityState} size={24} />
          <div className="flex items-center gap-1.5">
            <StatePill variant="reliability" state={stalk.reliabilityState} />
            <StatePill variant="latency" state={stalk.latencyState} />
          </div>
        </div>

        <section className="mb-10">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            Last {PULSE_HISTORY_LIMIT} {pluralize(PULSE_HISTORY_LIMIT, 'pulse')}
          </p>
          <div className="max-w-full overflow-x-auto">
            <div style={{ maxWidth: SPARKLINE_WIDTH_PX }}>
              <Sparkline
                pulses={pulses}
                reliabilityState={stalk.reliabilityState}
                slotCount={PULSE_HISTORY_LIMIT}
                barWidth={SPARKLINE_BAR_WIDTH}
                gap={SPARKLINE_GAP}
                height={110}
              />
              <div className="flex justify-between mt-2">
                <span className="font-mono text-[10px] tracking-wider text-ink-subtle">OLDEST</span>
                <span className="font-mono text-[10px] tracking-wider text-ink-subtle">NEWEST</span>
              </div>
            </div>
          </div>
        </section>

        <section className="mb-10">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            Config
          </p>
          <div className="rounded-lg border border-hairline bg-surface-raised py-6 px-6">
            <InfoRow label="URL" value={stalk.url} />
            <InfoRow label="Timeout" value={`${stalk.timeoutSeconds} seconds`} />
            <InfoRow label="Check every" value={`${stalk.growthIntervalSeconds} seconds`} />
            <InfoRow label="Created" value={formatRelativeTime(stalk.createdAt)} />
          </div>
        </section>

        <section>
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-3">
            Recent pulses ({pulses.length})
          </p>

          <div className="border border-hairline rounded-md overflow-hidden overflow-x-auto">
            <div className={`grid ${PULSE_GRID_COLS} gap-4 py-3 px-4 border-b border-hairline min-w-[410px]`}>
              <span className="font-mono text-xs uppercase tracking-wider text-ink-subtle">Time</span>
              <span className="font-mono text-xs uppercase tracking-wider text-ink-subtle">State</span>
              <span className="font-mono text-xs uppercase tracking-wider text-ink-subtle">Success</span>
              <span className="font-mono text-xs uppercase tracking-wider text-ink-subtle text-right">
                Latency
              </span>
              <span className="font-mono text-xs uppercase tracking-wider text-ink-subtle">Status</span>
            </div>

            {pulses.length === 0 ? (
              <p className="text-center text-ink-muted py-8">
                No pulses yet. Waiting for the first check…
              </p>
            ) : (
              pulses.map((pulse) => <PulseRow key={pulse.id} pulse={pulse} />)
            )}
          </div>
        </section>
      </main>

      <DeleteStalkModal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        stalkNickname={stalk.nickname}
        onConfirm={() => deleteMutation.mutate()}
        isDeleting={deleteMutation.isPending}
      />
    </div>
  );
}

function MetricCard({
  label,
  value,
  valueColorClass,
  secondary,
}: {
  label: string;
  value: string;
  valueColorClass: string;
  secondary: string;
}) {
  return (
    <div className="rounded-lg border border-hairline bg-surface-raised py-6 px-6">
      <p className="font-mono uppercase text-[11px] tracking-widest text-ink-subtle mb-2">
        {label}
      </p>
      <p className={`font-display font-normal text-[36px] leading-none ${valueColorClass}`}>
        {value}
      </p>
      <p className="font-mono text-[11px] text-ink-muted mt-2">{secondary}</p>
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-baseline justify-between py-2 border-b border-hairline last:border-b-0">
      <span className="font-mono text-xs uppercase tracking-wider text-ink-subtle">{label}</span>
      <span className="text-sm text-ink text-right break-all ml-4">{value}</span>
    </div>
  );
}

function PulseRow({ pulse }: { pulse: Pulse }) {
  const detail = pulse.errorMessage
    ? pulse.errorMessage
    : `Successful pulse — status ${pulse.statusCode ?? '—'}, ${pulse.latencyMs ?? '—'}ms`;

  return (
    <details className="group border-b border-hairline last:border-b-0">
      <summary
        className={`grid ${PULSE_GRID_COLS} items-center gap-4 py-3 px-4 min-w-[410px] hover:bg-surface-raised cursor-pointer list-none [&::-webkit-details-marker]:hidden`}
      >
        <span className="font-mono text-xs text-ink-muted">{formatRelativeTime(pulse.createdAt)}</span>
        <StatePill variant="reliability" state={pulse.reliabilityState} />
        <span aria-hidden="true">
          {pulse.isSuccess ? (
            <CheckIcon className="text-state-healthy" />
          ) : (
            <XIcon className="text-state-down" />
          )}
        </span>
        <span className="font-mono text-sm text-ink text-right">
          {pulse.latencyMs === null ? '—' : `${pulse.latencyMs}ms`}
        </span>
        <span className="font-mono text-sm text-ink">{pulse.statusCode ?? '—'}</span>
      </summary>
      <div className="px-4 pb-3 text-sm text-ink-muted min-w-[410px]">{detail}</div>
    </details>
  );
}

function StatusMessage({
  title,
  detail,
  onRetry,
}: {
  title: string;
  detail: string;
  onRetry?: () => void;
}) {
  return (
    <div className="min-h-screen bg-surface flex items-center justify-center px-6">
      <div className="w-full max-w-[400px] rounded-md border border-hairline bg-surface-raised p-8 text-center">
        <h1 className="font-display text-[24px] text-ink mb-2">{title}</h1>
        <p className="text-sm text-ink-muted leading-relaxed mb-6">{detail}</p>
        {onRetry ? (
          <button
            type="button"
            onClick={onRetry}
            className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-brand-fg"
          >
            Try again
          </button>
        ) : (
          <Link to="/dashboard" className="text-sm text-ink hover:underline">
            Back to dashboard
          </Link>
        )}
      </div>
    </div>
  );
}

function CheckIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" className={className}>
      <path d="M4 10.5l3.5 3.5L16 6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function XIcon({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 20" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" className={className}>
      <path d="M5 5l10 10M15 5L5 15" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
