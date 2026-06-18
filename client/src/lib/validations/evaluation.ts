import { z } from 'zod';

// ---------------------------------------------------------------------------
// QC Status values
// ---------------------------------------------------------------------------

export const QC_STATUSES = ['PASS', 'FAIL', 'NEED_FIX', 'IGNORED'] as const;

// ---------------------------------------------------------------------------
// Start Evaluation
// ---------------------------------------------------------------------------

export const startEvaluationSchema = z.object({
  datasetPublicId: z.string().min(1, 'required'),
  rubricVersionPublicId: z.string().min(1, 'required'),
  targetConnectorPublicId: z.string().min(1, 'required'),
  judgeModelPublicId: z.string().min(1, 'required'),
});

export type StartEvaluationFormValues = z.input<typeof startEvaluationSchema>;

// ---------------------------------------------------------------------------
// Review Decision (Epic 10 — schema defined here for shared use)
// ---------------------------------------------------------------------------

export const reviewDecisionSchema = z.object({
  qcStatus: z.enum(QC_STATUSES, { message: 'QC status is required' }),
  qcNote: z.string().max(2000).optional().or(z.literal('')),
  picBugUserPublicId: z.string().optional().or(z.literal('')),
});

export type ReviewDecisionFormValues = z.input<typeof reviewDecisionSchema>;
