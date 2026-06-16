import { z } from 'zod';

export const createRedTeamRunSchema = z.object({
  name: z.string().max(255, 'Tên lượt quét quá dài (tối đa 255 ký tự)').optional().or(z.literal('')),
  targetConnectorPublicId: z.string().min(1, 'API Connector mục tiêu là bắt buộc'),
  judgeModelPublicId: z.string().optional().or(z.literal('')),
  purpose: z
    .string()
    .min(10, 'Mục đích sử dụng của chatbot phải dài ít nhất 10 ký tự')
    .max(4000, 'Mục đích sử dụng quá dài (tối đa 4000 ký tự)'),
  plugins: z.array(z.string()).min(1, 'Vui lòng chọn ít nhất 1 loại lỗ hổng để quét'),
  numTests: z.number().min(1, 'Tối thiểu là 1').max(10, 'Tối đa là 10'),
});

export type CreateRedTeamRunFormValues = z.infer<typeof createRedTeamRunSchema>;
