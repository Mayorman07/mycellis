import { apiFetch } from './client';
import type { BatchPulsesResponse, PageResponse, Pulse, UptimeResponse } from '../types';

export function getRecentPulses(stalkId: string, limit: number): Promise<Pulse[]> {
  const query = new URLSearchParams({ limit: String(limit) });
  return apiFetch<Pulse[]>(`/stalks/${stalkId}/pulses?${query.toString()}`);
}

export function getPulseHistory(
  stalkId: string,
  params?: { page?: number; size?: number }
): Promise<PageResponse<Pulse>> {
  const query = new URLSearchParams();
  if (params?.page !== undefined) query.set('page', String(params.page));
  if (params?.size !== undefined) query.set('size', String(params.size));

  const qs = query.toString();
  return apiFetch<PageResponse<Pulse>>(`/stalks/${stalkId}/pulses/history${qs ? `?${qs}` : ''}`);
}

export function getUptime(stalkId: string, window: string): Promise<UptimeResponse> {
  const query = new URLSearchParams({ window });
  return apiFetch<UptimeResponse>(`/stalks/${stalkId}/pulses/uptime?${query.toString()}`);
}

export function getBatchPulses(stalkIds: string[], limit: number): Promise<BatchPulsesResponse> {
  if (stalkIds.length === 0) {
    return Promise.resolve({ pulsesByStalkId: {} });
  }
  const query = new URLSearchParams({ stalkIds: stalkIds.join(','), limit: String(limit) });
  return apiFetch<BatchPulsesResponse>(`/stalks/pulses/batch?${query.toString()}`);
}
