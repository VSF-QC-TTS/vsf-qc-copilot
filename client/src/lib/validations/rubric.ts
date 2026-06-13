import { z } from 'zod';

// ---------------------------------------------------------------------------
// Create Rubric
// ---------------------------------------------------------------------------

export const createRubricSchema = z.object({
  name: z.string().min(1, 'Rubric name is required').max(200, 'Name too long'),
  description: z.string().max(1000, 'Description too long').optional().or(z.literal('')),
  projectPublicId: z.string().min(1, 'Project is required'),
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
