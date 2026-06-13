import { z } from 'zod';

export const createRequirementSchema = z.object({
  content: z.string().min(1, 'Content is required').max(5000, 'Content too long'),
});

export const updateRequirementSchema = z.object({
  content: z.string().min(1, 'Content is required').max(5000, 'Content too long').optional(),
  status: z.enum(['ACTIVE', 'ARCHIVED']).optional(),
});

export type CreateRequirementFormValues = z.infer<typeof createRequirementSchema>;
export type UpdateRequirementFormValues = z.infer<typeof updateRequirementSchema>;
