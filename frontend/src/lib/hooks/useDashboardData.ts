import { useQuery, type UseQueryResult } from '@tanstack/react-query';
import { listStalks } from '../api/stalks';
import { getBatchPulses } from '../api/pulses';
import type { ApiError } from '../api/client';
import type { BatchPulsesResponse, PageResponse, Stalk } from '../types';

const REFETCH_INTERVAL_MS = 10_000;
const PULSES_PER_STALK = 40;

type DashboardData = {
  stalks: PageResponse<Stalk>;
  pulses: BatchPulsesResponse;
};

// Narrower than TanStack's full UseQueryResult — only the fields this app's
// consumers (DashboardPage, DashboardHeader's CommandPalette) actually read.
// stalksQuery and pulsesQuery below are both views over ONE underlying
// query now, so they share loading/error/refetch state by construction —
// that coupling (rather than two independently-timed fetches) is the whole
// point of this consolidation: it removes the window where reliabilityState
// has updated but the sparkline's pulse data hasn't, or vice versa.
type DerivedQuery<T> = {
  data: T | undefined;
  isLoading: boolean;
  isFetching: boolean;
  isError: boolean;
  error: ApiError | null;
  dataUpdatedAt: number;
  refetch: UseQueryResult<DashboardData, ApiError>['refetch'];
};

export function useDashboardData(): {
  stalksQuery: DerivedQuery<PageResponse<Stalk>>;
  pulsesQuery: DerivedQuery<BatchPulsesResponse>;
} {
  const dashboardQuery = useQuery<DashboardData, ApiError>({
    queryKey: ['dashboard'],
    queryFn: async () => {
      const stalks = await listStalks();
      // getBatchPulses needs stalk IDs, which only exist once listStalks()
      // resolves — these two calls have a hard data dependency, so this is
      // sequential, not Promise.all. Using the IDs from the response that
      // JUST arrived (not a stale previous render's list) is also strictly
      // more correct than the old two-query version's derivation.
      const stalkIds = stalks.content.map((s) => s.id);
      const pulses = await getBatchPulses(stalkIds, PULSES_PER_STALK);
      return { stalks, pulses };
    },
    refetchInterval: REFETCH_INTERVAL_MS,
    refetchIntervalInBackground: false,
  });

  const stalksQuery: DerivedQuery<PageResponse<Stalk>> = {
    data: dashboardQuery.data?.stalks,
    isLoading: dashboardQuery.isLoading,
    isFetching: dashboardQuery.isFetching,
    isError: dashboardQuery.isError,
    error: dashboardQuery.error,
    dataUpdatedAt: dashboardQuery.dataUpdatedAt,
    refetch: dashboardQuery.refetch,
  };

  const pulsesQuery: DerivedQuery<BatchPulsesResponse> = {
    data: dashboardQuery.data?.pulses,
    isLoading: dashboardQuery.isLoading,
    isFetching: dashboardQuery.isFetching,
    isError: dashboardQuery.isError,
    error: dashboardQuery.error,
    dataUpdatedAt: dashboardQuery.dataUpdatedAt,
    refetch: dashboardQuery.refetch,
  };

  return { stalksQuery, pulsesQuery };
}
