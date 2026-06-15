import { z } from 'zod';

// ---------------------------------------------------------------------------
// QC Status values
// ---------------------------------------------------------------------------

export const QC_STATUSES = ['PASS', 'FAIL', 'NEED_FIX', 'IGNORED'] as const;

// ---------------------------------------------------------------------------
// Start Evaluation
// ---------------------------------------------------------------------------

export const startEvaluationSchema = z.object({
  datasetPublicId: z.string().min(1, 'Dataset is required'),
  rubricVersionPublicId: z.string().min(1, 'Rubric version is required'),
  targetConnectorPublicId: z.string().min(1, 'Connector is required'),
  judgeModelPublicId: z.string().min(1, 'Judge model is required'),
});

export type StartEvaluationFormValues = z.input<typeof startEvaluationSchema>;

// ---------------------------------------------------------------------------
// Review Decision (Epic 10 — schema defined here for shared use)
// ---------------------------------------------------------------------------

export const reviewDecisionSchema = z.object({
  qcStatus: z.enum(QC_STATUSES, { message: 'QC status is required' }),
  qcNote: z.string().max(2000, 'Note too long').optional().or(z.literal('')),
  picBugUserPublicId: z.string().optional().or(z.literal('')),
});

export type ReviewDecisionFormValues = z.input<typeof reviewDecisionSchema>;
