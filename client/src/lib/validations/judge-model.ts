import { z } from 'zod';

export const createJudgeModelSchema = z.object({
  name: z.string().min(1, 'Name is required').max(200),
  provider: z.string().min(1, 'Provider is required'),
  modelName: z.string().min(1, 'Model name is required').max(200),
  apiKey: z.string().optional().or(z.literal('')),
  active: z.boolean().default(true),
  baseUrl: z
    .string()
    .optional()
    .or(z.literal(''))
    .refine(
      (val) => !val || val === '' || /^https?:\/\/.+/.test(val),
      { message: 'Must be a valid URL' },
    ),
  configJson: z
    .string()
    .optional()
    .or(z.literal(''))
    .refine(
      (val) => {
        if (!val || val === '') return true;
        try {
          JSON.parse(val);
          return true;
        } catch {
          return false;
        }
      },
      { message: 'Must be valid JSON' },
    ),
});

export type CreateJudgeModelFormValues = z.input<typeof createJudgeModelSchema>;
