import { apiFetch } from './client';
import type { CreateUserRequest, CreateUserResponse, LoginRequest, LoginResponse } from '../types';

export function login(credentials: LoginRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  });
}

export function signup(request: CreateUserRequest): Promise<CreateUserResponse> {
  return apiFetch<CreateUserResponse>('/users/create', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
