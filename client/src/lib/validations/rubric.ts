import { z } from 'zod';

// ---------------------------------------------------------------------------
// Generate / Create Rubric
// ---------------------------------------------------------------------------

export const generateRubricPreviewSchema = z.object({
  name: z.string().min(1, 'required').max(200),
  evaluationGoal: z
    .string()
    .min(1, 'required')
    .max(2000),
  domainContext: z.string().max(5000).optional().or(z.literal('')),
  language: z.string().max(20).optional().or(z.literal('')),
  sampleQuestion: z.string().max(2000).optional().or(z.literal('')),
  sampleExpectedAnswer: z
    .string()
    .max(5000)
    .optional()
    .or(z.literal('')),
  extraInstructions: z
    .string()
    .max(5000)
    .optional()
    .or(z.literal('')),
});

export type GenerateRubricPreviewFormValues = z.input<typeof generateRubricPreviewSchema>;

export const createRubricSchema = z.object({
  name: z.string().min(1, 'required').max(200),
  description: z.string().max(1000).optional().or(z.literal('')),
  content: z
    .string()
    .min(1, 'required')
    .max(10000),
  outputSchemaJson: z.string().max(10000).optional().or(z.literal('')),
});

export type CreateRubricFormValues = z.input<typeof createRubricSchema>;

// ---------------------------------------------------------------------------
// Create / Edit Criterion
// ---------------------------------------------------------------------------

export const createCriterionSchema = z.object({
  name: z.string().min(1, 'required').max(200),
  description: z.string().max(1000).optional().or(z.literal('')),
  weight: z.coerce
    .number()
    .int('Weight must be an integer')
    .min(1, 'required')
    .max(100),
  judgeInstruction: z.string().min(1, 'required').max(5000),
  passCondition: z.string().max(2000).optional().or(z.literal('')),
  failCondition: z.string().max(2000).optional().or(z.literal('')),
  metricKey: z
    .string()
    .min(1, 'required')
    .max(100)
    .regex(/^[a-z0-9_]+$/, 'Only lowercase letters, numbers, and underscores'),
  isCritical: z.boolean().default(false),
  sortOrder: z.coerce.number().int('Sort order must be an integer').min(0, 'Sort order must be non-negative'),
});

export type CreateCriterionFormValues = z.input<typeof createCriterionSchema>;
