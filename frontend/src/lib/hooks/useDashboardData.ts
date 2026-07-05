import { useQuery } from '@tanstack/react-query';
import { listStalks } from '../api/stalks';
import { getBatchPulses } from '../api/pulses';
import type { ApiError } from '../api/client';
import type { Pulse, Stalk } from '../types';

const REFETCH_INTERVAL_MS = 15_000;
const PULSES_PER_STALK = 40;

export function useDashboardData(): {
  stalks: Stalk[];
  pulsesByStalkId: Record<string, Pulse[]>;
  isLoading: boolean;
  isError: boolean;
  error: ApiError | null;
  lastSyncedAt: number;
} {
  const stalksQuery = useQuery({
    queryKey: ['stalks'],
    queryFn: () => listStalks(),
    refetchInterval: REFETCH_INTERVAL_MS,
  });

  const stalks = stalksQuery.data?.content ?? [];
  // Sorted so the queryKey is stable across renders even if the stalks response reorders.
  const sortedStalkIds = stalks.map((s) => s.id).sort();

  const pulsesQuery = useQuery({
    queryKey: ['pulses', 'batch', sortedStalkIds.join(',')],
    queryFn: () => getBatchPulses(sortedStalkIds, PULSES_PER_STALK),
    enabled: sortedStalkIds.length > 0,
    refetchInterval: REFETCH_INTERVAL_MS,
  });

  const pulsesByStalkId: Record<string, Pulse[]> = pulsesQuery.data?.pulsesByStalkId ?? {};

  return {
    stalks,
    pulsesByStalkId,
    isLoading: stalksQuery.isLoading || pulsesQuery.isLoading,
    isError: stalksQuery.isError || pulsesQuery.isError,
    error: (stalksQuery.error as ApiError) ?? (pulsesQuery.error as ApiError) ?? null,
    lastSyncedAt: stalksQuery.dataUpdatedAt,
  };
}
