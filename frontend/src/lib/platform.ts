// navigator.platform is deprecated but still the simplest reliable signal for
// this; userAgent is the fallback for the (rare) case it's empty.
export function isMacPlatform(): boolean {
  if (typeof navigator === 'undefined') return false;
  return /Mac|iPhone|iPad|iPod/.test(navigator.platform || navigator.userAgent);
}
