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
