import type { ProblemDetail } from '../types';

export class ApiError extends Error {
  status: number;
  type?: string;
  title: string;
  detail?: string;
  instance?: string;

  constructor(status: number, title: string, opts?: { type?: string; detail?: string; instance?: string }) {
    super(title);
    this.status = status;
    this.title = title;
    this.type = opts?.type;
    this.detail = opts?.detail;
    this.instance = opts?.instance;
  }
}

/** Accepts both 'me' and '/api/me' so callers don't have to think about the prefix. */
function resolvePath(path: string): string {
  if (path.startsWith('/api')) return path;
  return path.startsWith('/') ? `/api${path}` : `/api/${path}`;
}

export async function apiFetch<TResponse>(path: string, options: RequestInit = {}): Promise<TResponse> {
  const headers = new Headers(options.headers);
  if (options.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  let res: Response;
  try {
    res = await fetch(resolvePath(path), {
      ...options,
      headers,
      credentials: 'include',
    });
  } catch {
    throw new ApiError(0, 'Network error');
  }

  if (res.status === 204) {
    return undefined as TResponse;
  }

  if (!res.ok) {
    const problem: Partial<ProblemDetail> = await res.json().catch(() => ({}));
    throw new ApiError(res.status, problem.title ?? `Request failed with status ${res.status}`, {
      type: problem.type,
      detail: problem.detail,
      instance: problem.instance,
    });
  }

  return (await res.json()) as TResponse;
}
