import { apiFetch } from './client';
import type { MeResponse, UpdateAlertPreferencesRequest } from '../types';

export function getMe(): Promise<MeResponse> {
  return apiFetch<MeResponse>('/me');
}

export function updateAlertPreferences(request: UpdateAlertPreferencesRequest): Promise<MeResponse> {
  return apiFetch<MeResponse>('/me/alert-preferences', {
    method: 'PATCH',
    body: JSON.stringify(request),
  });
}
