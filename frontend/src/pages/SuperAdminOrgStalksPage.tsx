import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  getOrganizationDetail,
  getOrganizationStalks,
  getSuperAdminBatchPulses,
} from '../lib/api/superAdmin';
import type { ApiError } from '../lib/api/client';
import type { BatchPulsesResponse, SuperAdminOrgDetail, Stalk } from '../lib/types';
import { SuperAdminBanner } from '../components/superadmin/SuperAdminBanner';
import { StalkRow } from '../components/dashboard/StalkRow';

// Same header/grid shape as StalkTable, but not StalkTable itself — that
// component hardcodes navigate(`/stalks/${id}`) internally with no way to
// override it, and rows here must NOT navigate to the tenant-scoped detail
// page (no drill-down in this MVP). StalkRow is reused directly instead,
// since its onClick is a real, controllable prop.
const GRID_COLS = 'grid-cols-[2fr_1fr_2fr_0.5fr_0.5fr_0.3fr]';
const HEADER_LABELS = ['Stalk', 'State', 'Last 40 pulses', 'Latency', 'Uptime'];
const PULSES_PER_STALK = 40;

function noop() {}

export default function SuperAdminOrgStalksPage() {
  const { orgId } = useParams<{ orgId: string }>();

  const orgQuery = useQuery<SuperAdminOrgDetail, ApiError>({
    queryKey: ['super-admin', 'organization', orgId],
    queryFn: () => getOrganizationDetail(orgId!),
    enabled: !!orgId,
  });

  const stalksQuery = useQuery<Stalk[], ApiError>({
    queryKey: ['super-admin', 'organization', orgId, 'stalks'],
    queryFn: () => getOrganizationStalks(orgId!),
    enabled: !!orgId,
  });

  const stalkIds = stalksQuery.data?.map((s) => s.id) ?? [];
  const sortedStalkIds = [...stalkIds].sort();

  const pulsesQuery = useQuery<BatchPulsesResponse, ApiError>({
    queryKey: ['super-admin', 'pulses', 'batch', sortedStalkIds.join(',')],
    queryFn: () => getSuperAdminBatchPulses(sortedStalkIds, PULSES_PER_STALK),
    enabled: sortedStalkIds.length > 0,
  });

  if (orgQuery.isPending || stalksQuery.isPending) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center">
        <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">Loading…</p>
      </div>
    );
  }

  if (orgQuery.isError || stalksQuery.isError) {
    return (
      <div className="min-h-screen bg-surface flex items-center justify-center px-6">
        <div className="w-full max-w-[400px] rounded-md border border-hairline bg-surface-raised p-8 text-center">
          <p className="text-sm text-ink-muted mb-4">Couldn't load this organization's stalks.</p>
          <button
            type="button"
            onClick={() => {
              orgQuery.refetch();
              stalksQuery.refetch();
            }}
            className="rounded-md bg-brand px-4 py-2 text-sm font-medium text-brand-fg"
          >
            Try again
          </button>
        </div>
      </div>
    );
  }

  const org = orgQuery.data;
  const stalks = stalksQuery.data;

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-6xl mx-auto px-6 py-12">
        <div className="flex items-center justify-between mb-3">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">
            MYCELLIS · SUPER ADMIN · {org.name.toUpperCase()} · STALKS
          </p>
          <SuperAdminBanner />
        </div>
        <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-8">
          {org.name}'s stalks.
        </h1>

        {stalks.length === 0 ? (
          <p className="text-center text-ink-muted py-8 rounded-md border border-hairline bg-surface-raised">
            No stalks in this organization yet.
          </p>
        ) : (
          <div className="border border-hairline bg-surface-raised rounded-md overflow-hidden overflow-x-auto">
            <div className={`grid ${GRID_COLS} gap-4 py-3 px-4 border-b border-hairline min-w-[640px]`}>
              {HEADER_LABELS.map((label) => (
                <span key={label} className="font-mono text-xs uppercase tracking-wider text-ink-subtle">
                  {label}
                </span>
              ))}
            </div>
            {stalks.map((stalk) => (
              <StalkRow
                key={stalk.id}
                stalk={stalk}
                pulses={pulsesQuery.data?.pulsesByStalkId[stalk.id] ?? []}
                onClick={noop}
              />
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
