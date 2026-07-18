import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getStalk, updateStalk } from '../lib/api/stalks';
import type { ApiError } from '../lib/api/client';
import { StalkForm } from '../components/stalks/StalkForm';

type EditStalkErrorMessage = { title: string };

export default function EditStalkPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const stalkQuery = useQuery<Awaited<ReturnType<typeof getStalk>>, ApiError>({
    queryKey: ['stalk', id],
    queryFn: () => getStalk(id!),
    enabled: !!id,
  });

  const updateMutation = useMutation<
    Awaited<ReturnType<typeof updateStalk>>,
    ApiError,
    Parameters<typeof updateStalk>[1]
  >({
    mutationFn: (values) => updateStalk(id!, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['stalk', id] });
      queryClient.invalidateQueries({ queryKey: ['stalks'] });
      navigate(`/stalks/${id}`);
    },
  });

  const errorMessage = deriveErrorMessage(updateMutation.error);

  if (stalkQuery.isPending) {
    return (
      <div className="min-h-screen bg-surface">
        <main className="max-w-6xl mx-auto px-6 py-12">
          <div className="flex items-center gap-2 py-10">
            <span
              className="inline-block w-2 h-2 rounded-full bg-state-healthy"
              style={{ animation: 'mycellis-breath 2.6s ease-in-out infinite' }}
            />
            <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle">
              Loading stalk…
            </p>
          </div>
        </main>
      </div>
    );
  }

  if (stalkQuery.isError) {
    // Same status-code shape as StalkDetailPage's load error (Commit 14):
    // updateConfiguration's not-found path throws IllegalArgumentException
    // (400), never a literal 404; tenant mismatch is TenantAccessException (403).
    const title =
      stalkQuery.error.status === 400
        ? 'Stalk not found.'
        : stalkQuery.error.status === 403
          ? "You don't have access to this stalk."
          : "Couldn't load this stalk.";

    return (
      <div className="min-h-screen bg-surface flex items-center justify-center px-6">
        <div className="w-full max-w-[400px] rounded-md border border-hairline bg-surface-raised p-8 text-center">
          <h1 className="font-display text-[24px] text-ink mb-2">{title}</h1>
          <Link to="/dashboard" className="text-sm text-ink hover:underline">
            Back to dashboard
          </Link>
        </div>
      </div>
    );
  }

  const stalk = stalkQuery.data;

  return (
    <div className="min-h-screen bg-surface">
      <main className="max-w-6xl mx-auto px-6 py-12">
        <div className="max-w-[560px] mx-auto">
          <p className="font-mono uppercase text-xs tracking-wider text-ink-subtle mb-4">
            MYCELLIS · EDIT STALK
          </p>
          <h1 className="font-display font-normal text-[38px] leading-tight tracking-tight text-ink mb-3">
            Edit '{stalk.nickname}'.
          </h1>
          <p className="max-w-[500px] text-ink-muted leading-[1.5] mb-10">
            Update how Mycellis watches this endpoint.
          </p>

          <StalkForm
            mode="edit"
            initialValues={{
              nickname: stalk.nickname,
              url: stalk.url,
              timeoutSeconds: stalk.timeoutSeconds,
              growthIntervalSeconds: stalk.growthIntervalSeconds,
            }}
            onSubmit={(values) => updateMutation.mutate(values)}
            isSubmitting={updateMutation.isPending}
            errorMessage={errorMessage?.title ?? null}
            onCancel={() => navigate(`/stalks/${id}`)}
            submitLabel="Save changes"
            cancelLabel="Cancel"
          />
        </div>
      </main>
    </div>
  );
}

function deriveErrorMessage(error: ApiError | null): EditStalkErrorMessage | null {
  if (!error) {
    return null;
  }

  if (error.status === 0) {
    return { title: "Couldn't reach our servers. Check your connection." };
  }

  if (error.status === 429) {
    return { title: 'Too many changes recently. Try again in a moment.' };
  }

  if (error.status === 400) {
    return { title: error.detail || 'Please check your inputs and try again.' };
  }

  return { title: 'Something went wrong. Please try again.' };
}
