import { useEffect, useRef, useState, type KeyboardEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { StatePill } from './StatePill';
import type { Stalk } from '../../lib/types';

const MAX_RESULTS = 8;

type CommandPaletteProps = {
  onClose: () => void;
  stalks: Stalk[] | undefined;
  isLoading: boolean;
};

// Parent conditionally mounts this component only while open (see
// DashboardHeader), so every mount starts from fresh state by construction —
// no "reset on open" effect needed, and no isOpen prop to thread through.
export function CommandPalette({ onClose, stalks, isLoading }: CommandPaletteProps) {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState('');
  const [rawSelectedIndex, setRawSelectedIndex] = useState(0);

  const trimmedQuery = query.trim().toLowerCase();
  const results =
    trimmedQuery === '' || !stalks
      ? []
      : stalks
          .filter(
            (stalk) =>
              stalk.nickname.toLowerCase().includes(trimmedQuery) ||
              stalk.url.toLowerCase().includes(trimmedQuery)
          )
          .slice(0, MAX_RESULTS);

  // Derived, not synced via effect: clamping here (rather than in a
  // useEffect reacting to results.length) keeps the highlighted row valid
  // as the result set shrinks/grows while typing, with no extra render pass.
  const selectedIndex = Math.min(rawSelectedIndex, Math.max(0, results.length - 1));

  // Autofocus on mount.
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  // ESC closes.
  useEffect(() => {
    function handleKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  // Body scroll lock for the component's whole mounted lifetime.
  useEffect(() => {
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = '';
    };
  }, []);

  function selectStalk(stalk: Stalk) {
    onClose();
    navigate(`/stalks/${stalk.id}`);
  }

  function handleInputKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setRawSelectedIndex((i) => Math.min(i + 1, Math.max(0, results.length - 1)));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setRawSelectedIndex((i) => Math.max(i - 1, 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const selected = results[selectedIndex];
      if (selected) selectStalk(selected);
    }
  }

  return (
    <div className="fixed inset-0 z-[60] flex items-start justify-center px-4 pt-[15vh]">
      <div onClick={onClose} aria-hidden="true" className="fixed inset-0 bg-black/40" />

      <div
        role="dialog"
        aria-modal="true"
        aria-label="Search stalks"
        className="relative w-full max-w-lg rounded-lg border border-hairline bg-surface shadow-xl overflow-hidden"
      >
        <div className="border-b border-hairline px-4 py-3">
          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={handleInputKeyDown}
            placeholder="Search stalks by name or URL..."
            className="w-full bg-transparent text-base text-ink placeholder:text-ink-subtle focus:outline-none"
          />
        </div>

        <div className="max-h-[320px] overflow-y-auto py-2">
          {isLoading ? (
            <p className="px-4 py-6 text-center text-sm text-ink-subtle">Loading…</p>
          ) : trimmedQuery === '' ? (
            <p className="px-4 py-6 text-center text-sm text-ink-subtle">
              Type to search stalks...
            </p>
          ) : results.length === 0 ? (
            <p className="px-4 py-6 text-center text-sm text-ink-subtle">No stalks match</p>
          ) : (
            results.map((stalk, index) => (
              <button
                key={stalk.id}
                type="button"
                onClick={() => selectStalk(stalk)}
                onMouseEnter={() => setRawSelectedIndex(index)}
                className={`flex w-full items-center justify-between gap-3 px-4 py-2.5 text-left transition-colors ${
                  index === selectedIndex ? 'bg-surface-raised' : ''
                }`}
              >
                <div className="min-w-0">
                  <p className="font-semibold text-sm text-ink truncate">{stalk.nickname}</p>
                  <p className="font-mono text-xs text-ink-muted truncate">{stalk.url}</p>
                </div>
                <StatePill variant="reliability" state={stalk.reliabilityState} />
              </button>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
