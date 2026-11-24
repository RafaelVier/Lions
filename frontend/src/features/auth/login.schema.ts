import { z } from 'zod';
import type { LoginRequest } from '@/types/api';

export const loginSchema: z.ZodType<LoginRequest, LoginRequest> = z.object({
  username: z.string().trim().min(1, 'O usuário ou email é obrigatório.'),
  password: z.string().min(1, 'A senha é obrigatória.'),
});
