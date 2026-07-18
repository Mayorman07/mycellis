import { useEffect, useRef } from 'react';

type DeleteStalkModalProps = {
  isOpen: boolean;
  onClose: () => void;
  stalkNickname: string;
  onConfirm: () => void;
  isDeleting: boolean;
};

export function DeleteStalkModal({
  isOpen,
  onClose,
  stalkNickname,
  onConfirm,
  isDeleting,
}: DeleteStalkModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  // Native <dialog> open/closed state is imperative, not a plain boolean
  // attribute — showModal() is what gives us the backdrop and focus trap for
  // free, so isOpen has to be synced onto it via effect rather than JSX.
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) {
      return;
    }
    if (isOpen && !dialog.open) {
      dialog.showModal();
    } else if (!isOpen && dialog.open) {
      dialog.close();
    }
  }, [isOpen]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="delete-stalk-modal-title"
      // Escape and the native cancel action both fire "close" — route both
      // back through the same callback that keeps parent state in sync.
      onClose={onClose}
      // A click that reaches the <dialog> element itself (not something
      // inside it) landed on the ::backdrop — bubbling means the target is
      // only ever the dialog when the click didn't hit any inner content.
      onClick={(event) => {
        if (event.target === dialogRef.current) {
          onClose();
        }
      }}
      className="border-none bg-transparent p-0 backdrop:bg-[color-mix(in_srgb,var(--color-ink)_60%,transparent)]"
    >
      <div className="w-[min(480px,90vw)] rounded-lg border border-hairline bg-surface-raised p-8 shadow-[0_8px_30px_rgba(0,0,0,0.12)]">
        <h2 id="delete-stalk-modal-title" className="font-display text-[28px] text-ink mb-3">
          Stop watching {stalkNickname}?
        </h2>
        <p className="text-[15px] text-ink-muted leading-relaxed mb-8">
          This stops monitoring immediately and removes all pulse history. You can create a new
          stalk for the same URL anytime.
        </p>
        <div className="flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="rounded-md border border-hairline bg-surface-raised px-4 py-2 text-sm font-medium text-ink"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isDeleting}
            aria-busy={isDeleting}
            className="rounded-md bg-state-down px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-60"
          >
            {isDeleting ? 'Deleting…' : 'Delete'}
          </button>
        </div>
      </div>
    </dialog>
  );
}
