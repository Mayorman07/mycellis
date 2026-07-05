import { useQuery } from '@tanstack/react-query';
import { getMe } from '../api/me';
import type { ApiError } from '../api/client';
import type { MeResponse } from '../types';

export function useSession(): {
  data: MeResponse | undefined;
  isLoading: boolean;
  isError: boolean;
  error: ApiError | null;
  isAuthenticated: boolean;
} {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['me'],
    queryFn: getMe,
  });

  return {
    data,
    isLoading,
    isError,
    error: (error as ApiError) ?? null,
    isAuthenticated: !isLoading && !isError && data !== undefined,
  };
}
