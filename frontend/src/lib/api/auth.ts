import { apiFetch } from './client';
import type { LoginRequest, LoginResponse } from '../types';

export function login(credentials: LoginRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  });
}
