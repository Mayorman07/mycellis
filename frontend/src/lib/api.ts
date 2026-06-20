/**
 * Mycellis API client.
 *
 * Thin wrapper around fetch that handles:
 *  - Same-origin base URL (Vite proxy forwards to Spring backend)
 *  - Session cookies (credentials: 'include')
 *  - CSRF token (reads XSRF-TOKEN cookie, echoes in X-XSRF-TOKEN header)
 *  - JSON serialization
 *  - Throws ApiError on non-2xx responses
 */

const API_BASE = '/api';

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(status: number, message: string, body: unknown) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

/**
 * Reads a cookie value by name from document.cookie.
 * Returns null if not present.
 */
function readCookie(name: string): string | null {
  const match = document.cookie.match(
    new RegExp('(^| )' + name + '=([^;]+)')
  );
  return match ? decodeURIComponent(match[2]) : null;
}

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

interface RequestOptions {
  body?: unknown;
  headers?: Record<string, string>;
}

async function request<T = unknown>(
  method: HttpMethod,
  path: string,
  options: RequestOptions = {}
): Promise<T> {
  const url = `${API_BASE}${path}`;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  // CSRF: echo XSRF-TOKEN cookie in X-XSRF-TOKEN header for non-GET requests
  if (method !== 'GET') {
    const csrfToken = readCookie('XSRF-TOKEN');
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken;
    }
  }

  const res = await fetch(url, {
    method,
    headers,
    credentials: 'include',
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  // Parse response body if any
  const contentType = res.headers.get('content-type') || '';
  const isJson = contentType.includes('application/json');
  const body: unknown = isJson ? await res.json().catch(() => null) : null;

  if (!res.ok) {
    const message =
      isJson && body && typeof body === 'object' && 'detail' in body
        ? String((body as { detail: unknown }).detail)
        : `Request failed with status ${res.status}`;
    throw new ApiError(res.status, message, body);
  }

  return body as T;
}

export const api = {
  get: <T = unknown>(path: string, options?: RequestOptions) =>
    request<T>('GET', path, options),
  post: <T = unknown>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('POST', path, { ...options, body }),
  put: <T = unknown>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PUT', path, { ...options, body }),
  patch: <T = unknown>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PATCH', path, { ...options, body }),
  delete: <T = unknown>(path: string, options?: RequestOptions) =>
    request<T>('DELETE', path, options),
};