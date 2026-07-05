import { apiFetch } from './client';
import type { MeResponse } from '../types';

export function getMe(): Promise<MeResponse> {
  return apiFetch<MeResponse>('/me');
}
