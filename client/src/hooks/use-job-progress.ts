"use client";

import { useQuery } from "@tanstack/react-query";
import { apiClient } from "@/lib/api/client";
import type { JobResponse, JobStatus } from "@/lib/api/types";

const TERMINAL_STATUSES: JobStatus[] = ["COMPLETED", "FAILED", "CANCELLED"];
const POLL_INTERVAL_MS = 3_000;

type UseJobProgressOptions = {
  /** Called when job reaches COMPLETED status */
  onCompleted?: (job: JobResponse) => void;
  /** Called when job reaches FAILED or CANCELLED status */
  onFailed?: (job: JobResponse) => void;
  /** Whether polling is enabled (default: true) */
  enabled?: boolean;
};

/**
 * Poll GET /api/v1/jobs/{jobPublicId} every 3 seconds while the job is active.
 * Stops polling when status is COMPLETED, FAILED, or CANCELLED.
 */
export function useJobProgress(
  jobPublicId: string | null,
  options: UseJobProgressOptions = {}
) {
  const { onCompleted, onFailed, enabled = true } = options;

  const query = useQuery<JobResponse>({
    queryKey: ["job", jobPublicId],
    queryFn: async () => {
      const job = await apiClient.get<JobResponse>(
        `/api/v1/jobs/${jobPublicId}`
      );

      // Fire callbacks for terminal states
      if (job.status === "COMPLETED") {
        onCompleted?.(job);
      } else if (job.status === "FAILED" || job.status === "CANCELLED") {
        onFailed?.(job);
      }

      return job;
    },
    enabled: enabled && !!jobPublicId,
    refetchInterval: (query) => {
      const data = query.state.data;
      if (data && TERMINAL_STATUSES.includes(data.status)) {
        return false; // Stop polling
      }
      return POLL_INTERVAL_MS;
    },
    refetchIntervalInBackground: false,
  });

  const isTerminal =
    query.data != null && TERMINAL_STATUSES.includes(query.data.status);

  return {
    job: query.data ?? null,
    isLoading: query.isLoading,
    isPolling: query.isFetching && !isTerminal,
    isTerminal,
    isCompleted: query.data?.status === "COMPLETED",
    isFailed:
      query.data?.status === "FAILED" || query.data?.status === "CANCELLED",
    error: query.error,
  };
}
