import { z } from 'zod';

export const createProjectSchema = z.object({
  name: z.string().min(1, 'Project name is required').max(200, 'Name too long'),
  description: z.string().max(1000, 'Description too long').optional().or(z.literal('')),
});

export type CreateProjectFormValues = z.infer<typeof createProjectSchema>;
