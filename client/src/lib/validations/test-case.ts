import { z } from 'zod';

export const createTestCaseSchema = z.object({
  question: z.string().max(5000).optional().or(z.literal('')),
  turns: z.array(z.object({
    role: z.string(),
    content: z.string(),
  })).optional().nullable(),
  groundTruth: z.string().max(5000).optional().or(z.literal('')),
  precondition: z.string().max(2000).optional().or(z.literal('')).refine(
    (val) => {
      if (!val || val === '') return true;
      try {
        const parsed = JSON.parse(val);
        return typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed);
      } catch {
        return false;
      }
    },
    { message: 'Must be a valid JSON Object' }
  ),
  metadata: z.string().optional().or(z.literal('')).refine(
    (val) => {
      if (!val || val === '') return true;
      try {
        const parsed = JSON.parse(val);
        return typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed);
      } catch {
        return false;
      }
    },
    { message: 'Must be a valid JSON Object' }
  ),
});

export const generateTestCasesSchema = z.object({
  prompt: z.string().min(1, 'required').max(8000),
  count: z.coerce.number().int().min(5).max(100),
  additionalPrompt: z.string().max(4000).optional().or(z.literal('')),
});

export type CreateTestCaseFormValues = z.input<typeof createTestCaseSchema>;
export type GenerateTestCasesFormValues = z.input<typeof generateTestCasesSchema>;
