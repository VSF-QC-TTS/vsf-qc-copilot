import { z } from 'zod';

// ---------------------------------------------------------------------------
// Generate / Create Rubric
// ---------------------------------------------------------------------------

export const generateRubricPreviewSchema = z.object({
  name: z.string().min(1, 'Rubric name is required').max(200, 'Name too long'),
  evaluationGoal: z
    .string()
    .min(1, 'Evaluation goal is required')
    .max(2000, 'Evaluation goal too long'),
  domainContext: z.string().max(5000, 'Context too long').optional().or(z.literal('')),
  language: z.string().max(20, 'Language too long').optional().or(z.literal('')),
  sampleQuestion: z.string().max(2000, 'Sample question too long').optional().or(z.literal('')),
  sampleExpectedAnswer: z
    .string()
    .max(5000, 'Sample answer too long')
    .optional()
    .or(z.literal('')),
  extraInstructions: z
    .string()
    .max(5000, 'Instructions too long')
    .optional()
    .or(z.literal('')),
});

export type GenerateRubricPreviewFormValues = z.input<typeof generateRubricPreviewSchema>;

export const createRubricSchema = z.object({
  name: z.string().min(1, 'Rubric name is required').max(200, 'Name too long'),
  description: z.string().max(1000, 'Description too long').optional().or(z.literal('')),
  content: z
    .string()
    .min(1, 'Rubric content is required')
    .max(10000, 'Rubric content too long'),
  outputSchemaJson: z.string().max(10000, 'Output schema too long').optional().or(z.literal('')),
});

export type CreateRubricFormValues = z.input<typeof createRubricSchema>;

// ---------------------------------------------------------------------------
// Create / Edit Criterion
// ---------------------------------------------------------------------------

export const createCriterionSchema = z.object({
  name: z.string().min(1, 'Criterion name is required').max(200, 'Name too long'),
  description: z.string().max(1000, 'Description too long').optional().or(z.literal('')),
  weight: z.coerce
    .number()
    .int('Weight must be an integer')
    .min(1, 'Weight must be at least 1')
    .max(100, 'Weight must be at most 100'),
  judgeInstruction: z.string().min(1, 'Judge instruction is required').max(5000, 'Instruction too long'),
  passCondition: z.string().max(2000, 'Pass condition too long').optional().or(z.literal('')),
  failCondition: z.string().max(2000, 'Fail condition too long').optional().or(z.literal('')),
  metricKey: z
    .string()
    .min(1, 'Metric key is required')
    .max(100, 'Metric key too long')
    .regex(/^[a-z0-9_]+$/, 'Only lowercase letters, numbers, and underscores'),
  isCritical: z.boolean().default(false),
  sortOrder: z.coerce.number().int('Sort order must be an integer').min(0, 'Sort order must be non-negative'),
});

export type CreateCriterionFormValues = z.input<typeof createCriterionSchema>;
