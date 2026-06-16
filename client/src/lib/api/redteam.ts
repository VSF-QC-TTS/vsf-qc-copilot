import { apiClient } from "@/lib/api/client";
import type {
  CreateRedTeamRunRequest,
  CreateRedTeamRunResponse,
  RedTeamRunResponse,
  RedTeamResultResponse,
  PageResponse,
} from "@/lib/api/types";

/** POST /api/v1/projects/{projectId}/red-team-runs */
export async function createRedTeamRun(
  projectId: string,
  data: CreateRedTeamRunRequest,
): Promise<CreateRedTeamRunResponse> {
  return apiClient.post<CreateRedTeamRunResponse>(
    `/api/v1/projects/${projectId}/red-team-runs`,
    data,
  );
}

/** GET /api/v1/projects/{projectId}/red-team-runs */
export async function listRedTeamRuns(
  projectId: string,
  page = 0,
  size = 20,
): Promise<PageResponse<RedTeamRunResponse>> {
  return apiClient.get<PageResponse<RedTeamRunResponse>>(
    `/api/v1/projects/${projectId}/red-team-runs?page=${page}&size=${size}&sort=createdAt,desc`,
  );
}

/** GET /api/v1/red-team-runs/{runId} */
export async function getRedTeamRun(runId: string): Promise<RedTeamRunResponse> {
  return apiClient.get<RedTeamRunResponse>(`/api/v1/red-team-runs/${runId}`);
}

/** GET /api/v1/red-team-runs/{runId}/results */
export async function getRedTeamResults(runId: string): Promise<RedTeamResultResponse> {
  return apiClient.get<RedTeamResultResponse>(`/api/v1/red-team-runs/${runId}/results`);
}
