import { apiFetch } from './client';
import type { PageResponse, Stalk } from '../types';

export function listStalks(params?: { page?: number; size?: number }): Promise<PageResponse<Stalk>> {
  const query = new URLSearchParams();
  if (params?.page !== undefined) query.set('page', String(params.page));
  if (params?.size !== undefined) query.set('size', String(params.size));

  const qs = query.toString();
  return apiFetch<PageResponse<Stalk>>(`/stalks${qs ? `?${qs}` : ''}`);
}

export function getStalk(id: string): Promise<Stalk> {
  return apiFetch<Stalk>(`/stalks/${id}`);
}
