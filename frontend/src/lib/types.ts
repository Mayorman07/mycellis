/**
 * Type definitions that mirror Spring backend DTOs.
 *
 * Keep these in sync with backend models. When the API contract changes,
 * update here too.
 */

// Auth
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  userId: string;
  email: string;
  roles: string[];
}

export interface VerifyEmailRequest {
  token: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ResendVerificationRequest {
  email: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ChangeEmailRequest {
  currentPassword: string;
  newEmail: string;
}

// User signup
export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  mobileNumber?: string;
  gender?: string;
}

export interface CreateUserResponse {
  userId: string;
  email: string;
  status: string;
}

// Generic error shape from your GlobalExceptionHandler
export interface ApiErrorBody {
  type?: string;
  title?: string;
  status: number;
  detail: string;
  instance?: string;
  timestamp?: string;
}