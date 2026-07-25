import { apiFetch } from './client';
import type {
  BatchPulsesResponse,
  SuperAdminOrgDetail,
  SuperAdminOrgSummary,
  SuperAdminUserSummary,
} from '../types';
import type { Stalk } from '../types';

export function getAllOrganizations(): Promise<SuperAdminOrgSummary[]> {
  return apiFetch<SuperAdminOrgSummary[]>('/super-admin/organizations');
}

export function getAllUsers(): Promise<SuperAdminUserSummary[]> {
  return apiFetch<SuperAdminUserSummary[]>('/super-admin/users');
}

export function getOrganizationDetail(orgId: string): Promise<SuperAdminOrgDetail> {
  return apiFetch<SuperAdminOrgDetail>(`/super-admin/organizations/${orgId}`);
}

export function getOrganizationStalks(orgId: string): Promise<Stalk[]> {
  return apiFetch<Stalk[]>(`/super-admin/organizations/${orgId}/stalks`);
}

// The tenant-scoped equivalent (getBatchPulses in lib/api/pulses.ts) is a GET
// with query params, not a POST with a body — this mirrors that actual shape
// rather than inventing a different one for the super-admin path.
export function getSuperAdminBatchPulses(
  stalkIds: string[],
  limit: number
): Promise<BatchPulsesResponse> {
  if (stalkIds.length === 0) {
    return Promise.resolve({ pulsesByStalkId: {} });
  }
  const query = new URLSearchParams({ stalkIds: stalkIds.join(','), limit: String(limit) });
  return apiFetch<BatchPulsesResponse>(`/super-admin/stalks/pulses/batch?${query.toString()}`);
}
