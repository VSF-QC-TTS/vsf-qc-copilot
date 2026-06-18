import { z } from 'zod';

export const createProjectSchema = z.object({
  name: z.string().min(1, 'required').max(200),
  description: z.string().max(1000).optional().or(z.literal('')),
});

export type CreateProjectFormValues = z.infer<typeof createProjectSchema>;
