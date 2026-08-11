// Mirrors the backend's @Pattern on email fields (see CreateUserRequest and
// its siblings in user.model.request) — @Email's default regex alone is too
// permissive and accepts TLD-less addresses like "user@yopmail". Keep this
// in sync with that regex; this is UX only, the backend is the source of
// truth for what's actually accepted.
export const EMAIL_PATTERN = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

export function isValidEmail(value: string): boolean {
  return EMAIL_PATTERN.test(value);
}
