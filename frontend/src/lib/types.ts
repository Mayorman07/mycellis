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
  rememberMe?: boolean;
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

// Problem Details (RFC 9457), as returned by the global exception handler
export type ProblemDetail = {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  timestamp?: string;
};

// Dashboard domain

export type PlanTier = 'FREE' | 'PRO' | 'ENTERPRISE';

export type ReliabilityState = 'HEALTHY' | 'DEGRADED' | 'DOWN' | 'DORMANT';

export type LatencyState = 'NORMAL' | 'STRESSED';

export type User = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
};

export type Organization = {
  id: string;
  name: string;
  slug: string;
  planTier: PlanTier;
};

export type MeResponse = {
  user: User;
  organization: Organization;
};

export type Stalk = {
  id: string;
  organizationId: string;
  createdByUserId: string;
  nickname: string;
  url: string;
  reliabilityState: ReliabilityState;
  latencyState: LatencyState;
  averageLatencyMs: number | null;
  healthIndex: number;
  consecutiveFailures: number;
  growthIntervalSeconds: number;
  timeoutSeconds: number;
  /** @deprecated legacy pre-V10 state field, retained for backward compat */
  currentState: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type Pulse = {
  id: string;
  stalkId: string;
  reliabilityState: ReliabilityState;
  latencyState: LatencyState;
  latencyMs: number | null;
  statusCode: number | null;
  errorMessage: string | null;
  createdAt: string;
};

export type UptimeResponse = {
  uptimePercent: number;
  windowStart: string;
  windowEnd: string;
};

export type BatchPulsesResponse = {
  pulsesByStalkId: Record<string, Pulse[]>;
};

// Mirrors Spring's Page<T> JSON shape
export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
};