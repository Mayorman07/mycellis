import { apiFetch } from './client';
import type {
  CreateUserRequest,
  CreateUserResponse,
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  ResetPasswordRequest,
} from '../types';

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

export function forgotPassword(request: ForgotPasswordRequest): Promise<void> {
  return apiFetch<void>('/auth/forgot-password', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}

export function resetPassword(request: ResetPasswordRequest): Promise<void> {
  return apiFetch<void>('/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(request),
  });
}
