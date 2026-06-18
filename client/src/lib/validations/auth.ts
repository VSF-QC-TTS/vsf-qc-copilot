import { z } from "zod";

/** Login form schema */
export const loginSchema = z.object({
  email: z
    .string()
    .min(1, 'required')
    .pipe(z.email("Invalid email address")),
  password: z
    .string()
    .min(1, 'required'),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

/** Registration form schema */
export const registerSchema = z
  .object({
    email: z
      .string()
      .min(1, 'required')
      .pipe(z.email("Invalid email address")),
    fullName: z
      .string()
      .min(1, 'required')
      .max(100),
    password: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .regex(/[A-Z]/, "Password must contain an uppercase letter")
      .regex(/[a-z]/, "Password must contain a lowercase letter")
      .regex(/[0-9]/, "Password must contain a number"),
    confirmPassword: z
      .string()
      .min(1, 'required'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

export type RegisterFormValues = z.infer<typeof registerSchema>;

/** Forgot password schema */
export const forgotPasswordSchema = z.object({
  email: z
    .string()
    .min(1, 'required')
    .pipe(z.email("Invalid email address")),
});

export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;

/** Reset password schema */
export const resetPasswordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .regex(/[A-Z]/, "Password must contain an uppercase letter")
      .regex(/[a-z]/, "Password must contain a lowercase letter")
      .regex(/[0-9]/, "Password must contain a number"),
    confirmPassword: z
      .string()
      .min(1, 'required'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
