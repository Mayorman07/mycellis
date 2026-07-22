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
// Mirrors backend com.mycelis.user.constant.Gender exactly — note OTHER, not
// NON_BINARY (the backend has no dedicated non-binary value).
export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  mobileNumber: string;
  gender: Gender;
  organizationName: string;
}

export interface CreateUserResponse {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  status: string;
  roles: string[];
  createdAt: string;
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
  createdAt: string;
};

export type Organization = {
  id: string;
  name: string;
  slug: string;
  planTier: PlanTier;
  memberCount: number;
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

export type CreateStalkRequest = {
  nickname: string;
  url: string;
  timeoutSeconds: number;
  growthIntervalSeconds: number;
};

export type Pulse = {
  id: string;
  stalkId: string;
  reliabilityState: ReliabilityState;
  latencyState: LatencyState;
  latencyMs: number | null;
  statusCode: number | null;
  isSuccess: boolean;
  errorMessage: string | null;
  createdAt: string;
};

// Public status page (no auth) — deliberately excludes anything the backend
// itself doesn't expose publicly: stalk URLs, stalk/org ids, plan tier,
// member count. See com.mycelis.status.dto.PublicStatusResponse.
export type OverallState = 'HEALTHY' | 'STRESSED' | 'DEGRADED' | 'IMPAIRED';

export type PublicStalkStatus = {
  nickname: string;
  reliabilityState: ReliabilityState;
  latencyState: LatencyState;
  healthIndex: number;
  averageLatencyMs: number;
  uptimeHistory90d: (number | null)[];
};

export type PublicStatusResponse = {
  organization: { name: string; slug: string };
  overallState: OverallState;
  stalks: PublicStalkStatus[];
  lastUpdated: string;
};

// Super admin (read-only, cross-tenant) — see com.mycelis.superadmin.dto.
export type SuperAdminOrgSummary = {
  id: string;
  name: string;
  slug: string;
  planTier: string;
  memberCount: number;
  stalkCount: number;
  createdAt: string;
};

export type SuperAdminUserSummary = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  organizationName: string | null;
  roles: string[];
  status: string;
  createdAt: string;
};

export type SuperAdminOrgMember = {
  userId: string;
  name: string;
  email: string;
  role: string;
  isPrimary: boolean;
};

export type SuperAdminOrgDetail = {
  id: string;
  name: string;
  slug: string;
  planTier: string;
  memberCount: number;
  stalkCount: number;
  createdAt: string;
  members: SuperAdminOrgMember[];
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