import { z } from 'zod';

export const createRedTeamRunSchema = z.object({
  name: z.string().max(255).optional().or(z.literal('')),
  targetConnectorPublicId: z.string().min(1, 'required'),
  judgeModelPublicId: z.string().optional().or(z.literal('')),
  purpose: z
    .string()
    .min(10)
    .max(4000),
  plugins: z.array(z.string()).min(1),
  numTests: z.number().min(1).max(10),
});

export type CreateRedTeamRunFormValues = z.infer<typeof createRedTeamRunSchema>;
