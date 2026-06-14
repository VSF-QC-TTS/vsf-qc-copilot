import { z } from 'zod';

export const createTestCaseSchema = z.object({
  question: z.string().min(1, 'Question is required').max(5000),
  groundTruth: z.string().max(5000).optional().or(z.literal('')),
  precondition: z.string().max(2000).optional().or(z.literal('')),
  metadata: z.string().optional().or(z.literal('')).refine(
    (val) => { if (!val || val === '') return true; try { JSON.parse(val); return true; } catch { return false; } },
    { message: 'Must be valid JSON' }
  ),
});

export const generateTestCasesSchema = z.object({
  prompt: z.string().min(1, 'Prompt is required').max(8000),
  count: z.coerce.number().int().min(5).max(100),
  additionalPrompt: z.string().max(4000).optional().or(z.literal('')),
});

export type CreateTestCaseFormValues = z.input<typeof createTestCaseSchema>;
export type GenerateTestCasesFormValues = z.input<typeof generateTestCasesSchema>;
