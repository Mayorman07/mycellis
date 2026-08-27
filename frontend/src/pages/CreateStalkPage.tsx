import { useNavigate } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createStalk } from '../lib/api/stalks';
import type { ApiError } from '../lib/api/client';
import { getApiErrorMessage } from '../lib/apiErrorMessage';
import { useRetryCountdown } from '../lib/hooks/useRetryCountdown';
import { StalkForm } from '../components/stalks/StalkForm';

type CreateStalkErrorMessage = { title: string };

export default function CreateStalkPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const createMutation = useMutation<
    Awaited<ReturnType<typeof createStalk>>,
    ApiError,
    Parameters<typeof createStalk>[0]
  >({
    mutationFn: createStalk,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stalks'] });
      navigate('/dashboard', { replace: true });
    },
  });

  const retrySecondsRemaining = useRetryCountdown(createMutation.error?.retryAfterSeconds ?? null);
  const errorMessage = deriveErrorMessage(createMutation.error, retrySecondsRemaining);

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-6xl mx-auto px-6 py-12">
        <div className="max-w-[560px] mx-auto">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-4">
            MYCELLIS · NEW STALK
          </p>
          <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-3">
            Plant a new stalk.
          </h1>
          <p className="max-w-[500px] text-ink-muted leading-[1.5] mb-10">
            Point Mycellis at any endpoint. We'll listen and tell you how it's breathing.
          </p>

          <StalkForm
            mode="create"
            onSubmit={(values) => createMutation.mutate(values)}
            isSubmitting={createMutation.isPending}
            errorMessage={errorMessage?.title ?? null}
            onCancel={() => navigate('/dashboard')}
            submitLabel="Plant stalk"
            cancelLabel="Cancel and go back"
          />
        </div>
      </main>
    </div>
  );
}

function deriveErrorMessage(error: ApiError | null, retrySecondsRemaining: number): CreateStalkErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers. Check your connection." };
  }

  if (error.status === 429) {
    if (retrySecondsRemaining <= 0) {
      return { title: 'You can try again now.' };
    }
    const unit = retrySecondsRemaining === 1 ? 'second' : 'seconds';
    return { title: `You've created stalks too quickly. Try again in ${retrySecondsRemaining} ${unit}.` };
  }

  if (error.status === 400) {
    return { title: getApiErrorMessage(error, 'Please check your inputs and try again.') };
  }

  return { title: getApiErrorMessage(error) };
}
