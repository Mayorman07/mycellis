import type { Stalk } from '../../lib/types';

type KpiStripProps = {
  stalks: Stalk[];
};

function computeFleetUptime(stalks: Stalk[]): string {
  const nonDormant = stalks.filter((s) => s.reliabilityState !== 'DORMANT');
  if (nonDormant.length === 0) return '—';
  const healthy = nonDormant.filter((s) => s.reliabilityState === 'HEALTHY').length;
  return ((healthy / nonDormant.length) * 100).toFixed(1);
}

export function KpiStrip({ stalks }: KpiStripProps) {
  const fleetUptime = computeFleetUptime(stalks);
  const stressedCount = stalks.filter((s) => s.latencyState === 'STRESSED').length;
  const incidentsCount = stalks.filter(
    (s) => s.reliabilityState === 'DOWN' || s.reliabilityState === 'DEGRADED'
  ).length;

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      <KpiCard label="Fleet Uptime" value={fleetUptime} suffix={fleetUptime === '—' ? undefined : '%'} />
      <KpiCard label="Stalks" value={String(stalks.length)} />
      <KpiCard label="Stressed" value={String(stressedCount)} valueClassName="text-state-stressed" />
      <KpiCard label="Incidents" value={String(incidentsCount)} valueClassName="text-state-down" />
    </div>
  );
}

function KpiCard({
  label,
  value,
  suffix,
  valueClassName = 'text-ink',
}: {
  label: string;
  value: string;
  suffix?: string;
  valueClassName?: string;
}) {
  return (
    <div className="rounded-md border border-hairline bg-surface-raised p-5">
      <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">{label}</p>
      <p className={`font-display text-[48px] leading-tight mt-1 ${valueClassName}`}>
        {value}
        {suffix && <span className="text-xl align-top">{suffix}</span>}
      </p>
    </div>
  );
}
