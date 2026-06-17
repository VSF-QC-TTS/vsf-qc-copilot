// ---------------------------------------------------------------------------
// Pagination
// ---------------------------------------------------------------------------
export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

// ---------------------------------------------------------------------------
// User
// ---------------------------------------------------------------------------
export type UserRole = 'QC_MEMBER' | 'QC_LEAD' | 'ADMIN';
export type UserStatus = 'PENDING_EMAIL_VERIFICATION' | 'ACTIVE' | 'DISABLED';

export type UserResponse = {
  publicId: string;
  email: string;
  displayName: string;
  role: UserRole;
  status: UserStatus;
  avatarUrl: string | null;
  lastLoginAt: string | null;
};

// ---------------------------------------------------------------------------
// Auth
// ---------------------------------------------------------------------------
export type LoginRequest = { email: string; password: string };

export type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserResponse;
};

export type RefreshTokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
};

// ---------------------------------------------------------------------------
// Project
// ---------------------------------------------------------------------------
export type ProjectStatus = 'ACTIVE' | 'ARCHIVED';

export type ProjectResponse = {
  publicId: string;
  name: string;
  description: string | null;
  status: ProjectStatus;
  createdAt: string;
  updatedAt: string;
  archivedAt: string | null;
};

// ---------------------------------------------------------------------------
// Evaluation / QC
// ---------------------------------------------------------------------------
export type EvaluationRunStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type JobStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type JudgeStatus = 'PASS' | 'FAIL' | 'WARNING' | 'ERROR';
export type QcStatus = 'NOT_REVIEWED' | 'PASS' | 'FAIL' | 'NEED_FIX' | 'IGNORED';
export type DatasetStatus = 'DRAFT' | 'APPROVED' | 'ARCHIVED';
export type TestCaseStatus = 'ACTIVE' | 'INACTIVE';
export type RubricVersionStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type ExportStatus = 'PENDING' | 'PROCESSING' | 'READY' | 'FAILED';
export type JudgeProvider =
  | 'GEMINI'
  | 'OPENAI'
  | 'ANTHROPIC'
  | 'DEEPSEEK'
  | 'CUSTOM';

export type JudgeModelResponse = {
  publicId: string;
  projectPublicId: string;
  name: string;
  provider: JudgeProvider;
  modelName: string;
  baseUrl: string | null;
  apiKeyMasked: string;
  configJson: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export type JobResponse = {
  publicId: string;
  jobType: string;
  status: JobStatus;
  resourcePublicId: string | null;
  progressCurrent: number;
  progressTotal: number;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
};

export type JobEventResponse = {
  publicId: string;
  eventType: string;
  payloadJson: string | null;
  createdAt: string;
};

// ---------------------------------------------------------------------------
// API Error (Problem Details)
// ---------------------------------------------------------------------------
export type ApiFieldError = {
  field: string;
  message: string;
};

export type ApiError = {
  status: number;
  code: string;
  message: string;
  title?: string;
  detail?: string;
  errors?: ApiFieldError[];
};

// ---------------------------------------------------------------------------
// Evaluation Run Detail
// ---------------------------------------------------------------------------
export type EvaluationRunDetail = {
  publicId: string;
  datasetPublicId: string;
  datasetName: string | null;
  rubricVersionPublicId: string;
  rubricName: string | null;
  rubricVersionNumber: number;
  targetConnectorPublicId: string;
  connectorName: string | null;
  judgeModelPublicId: string | null;
  judgeModelDisplayName: string | null;
  jobPublicId: string | null;
  status: string;
  description: string | null;
  totalCases: number;
  completedCases: number;
  passedCases: number;
  failedCases: number;
  warningCases: number;
  errorCases: number;
  passRate: number;
  maxConcurrency: number;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Dataset Detail Response
// ---------------------------------------------------------------------------
export type DatasetDetailResponse = {
  publicId: string;
  name: string;
  description: string | null;
  status: DatasetStatus;
  testCaseCount: number;
  activeTestCaseCount: number;
  createdAt: string;
  updatedAt: string;
};

// ---------------------------------------------------------------------------
// Red-Team
// ---------------------------------------------------------------------------
export type RedTeamRunStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';

export type CreateRedTeamRunRequest = {
  name?: string;
  targetConnectorPublicId: string;
  judgeModelPublicId?: string | null;
  purpose: string;
  plugins?: string[];
  strategies?: string[];
  numTests?: number;
};

export type CreateRedTeamRunResponse = {
  runPublicId: string;
  jobPublicId: string;
  status: string;
  message: string;
};

export type RedTeamRunResponse = {
  publicId: string;
  projectPublicId: string;
  targetConnectorPublicId: string;
  connectorName: string | null;
  judgeModelPublicId: string | null;
  judgeModelDisplayName: string | null;
  jobPublicId: string | null;
  name: string;
  purpose: string;
  plugins: string[];
  strategies: string[];
  numTests: number;
  status: RedTeamRunStatus;
  totalCases: number;
  passedCases: number;
  failedCases: number;
  errorCases: number;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type RedTeamResultItem = {
  pluginId: string;
  prompt: string;
  response: string;
  score: number;
  pass: boolean;
  reason: string;
  graderName?: string;
};

export type RedTeamResultResponse = {
  runPublicId: string;
  status: string;
  summary: {
    successes: number;
    failures: number;
    errors: number;
    tokenUsage?: {
      total: number;
      prompt: number;
      completion: number;
    };
  };
  results: {
    stats: {
      successes: number;
      failures: number;
      errors: number;
      tokenUsage?: {
        total: number;
        prompt: number;
        completion: number;
      };
    };
    results: Array<{
      pluginId: string;
      prompt: {
        raw: string;
        label: string;
      } | string;
      vars: Record<string, unknown>;
      response: {
        raw: string;
        data?: unknown;
      } | string;
      success: boolean;
      score: number;
      latencyMs?: number;
      gradingResult?: {
        pass: boolean;
        score: number;
        reason: string;
        componentResults?: unknown[];
        assertion?: unknown;
      };
      provider?: {
        id: string;
      };
    }>;
  } | null;
};

// ---------------------------------------------------------------------------
// Evaluation Result (used in result list + detail panel)
// ---------------------------------------------------------------------------
export type CriterionResult = {
  metricKey?: string | null;
  name: string;
  status: string;
  score: number | null;
  reason: string | null;
  graderError?: boolean;
};

export type EvaluationResultRow = {
  publicId: string;
  question: string;
  precondition: string | null;
  groundTruth: string | null;
  actualAnswer: string | null;
  judgeStatus: string | null;
  judgeScore: number | null;
  criteriaResultsJson: string | null;
  criteriaResults?: CriterionResult[] | null;
  qcStatus: string;
  qcNote: string | null;
};
