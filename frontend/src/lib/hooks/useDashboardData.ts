import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { listStalks } from '../api/stalks';
import { getBatchPulses } from '../api/pulses';
import type { ApiError } from '../api/client';
import type { BatchPulsesResponse, PageResponse, Stalk } from '../types';

const REFETCH_INTERVAL_MS = 10_000;
const PULSES_PER_STALK = 40;

export function useDashboardData(): {
  stalksQuery: UseQueryResult<PageResponse<Stalk>, ApiError>;
  pulsesQuery: UseQueryResult<BatchPulsesResponse, ApiError>;
} {
  const stalksQuery = useQuery<PageResponse<Stalk>, ApiError>({
    queryKey: ['stalks'],
    queryFn: () => listStalks(),
    refetchInterval: REFETCH_INTERVAL_MS,
    // Pauses polling when the tab isn't focused — no point burning requests
    // on a dashboard nobody's looking at.
    refetchIntervalInBackground: false,
  });

  const stalkIds = stalksQuery.data?.content.map((s) => s.id) ?? [];
  // Sorted so the queryKey is stable across renders even if the stalks response reorders.
  const sortedStalkIds = [...stalkIds].sort();

  const pulsesQuery = useQuery<BatchPulsesResponse, ApiError>({
    queryKey: ['pulses', 'batch', sortedStalkIds.join(',')],
    queryFn: () => getBatchPulses(sortedStalkIds, PULSES_PER_STALK),
    enabled: sortedStalkIds.length > 0,
    refetchInterval: REFETCH_INTERVAL_MS,
    refetchIntervalInBackground: false,
  });

  return { stalksQuery, pulsesQuery };
}
