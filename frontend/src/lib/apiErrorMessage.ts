import type { ApiError } from './api/client';

const DEFAULT_FALLBACK = 'Something went wrong. Please try again.';

/**
 * Reads the RFC 7807 `detail` field apiFetch already captures on every
 * ApiError and returns it for display. Falls back to a generic message
 * when detail is missing or blank (network failures via status 0, or any
 * response that genuinely has nothing more specific to say).
 */
export function getApiErrorMessage(error: ApiError | null | undefined, fallback: string = DEFAULT_FALLBACK): string {
  const detail = error?.detail?.trim();
  return detail ? detail : fallback;
}
