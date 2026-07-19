import { apiFetch } from './client';
import type { PublicStatusResponse } from '../types';

export function getPublicStatus(slug: string): Promise<PublicStatusResponse> {
  return apiFetch<PublicStatusResponse>(`/status/${slug}`);
}
