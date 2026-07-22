import { apiFetch } from './client';
import type {
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
