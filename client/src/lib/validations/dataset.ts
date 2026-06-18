import { z } from 'zod';

export const createDatasetSchema = z.object({
  name: z.string().min(1, 'required').max(200),
  description: z.string().max(1000).optional().or(z.literal('')),

  sourceType: z.literal('MANUAL').default('MANUAL'),
});

export type CreateDatasetFormValues = z.input<typeof createDatasetSchema>;
