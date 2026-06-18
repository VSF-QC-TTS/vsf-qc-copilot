"use client";

import { Suspense, useState } from "react";
import { useTranslations } from "next-intl";
import { useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link } from "@/i18n/navigation";
import { Button } from "@/components/ui/button";
import { FloatingPasswordInput } from "@/components/ui/floating-input";
import { cn } from "@/lib/utils";
import { resetPassword } from "@/lib/api/auth";
import { getErrorMessageKey } from "@/lib/utils/error-messages";
import type { ApiError } from "@/lib/api/types";
import {
  resetPasswordSchema,
  type ResetPasswordFormValues,
} from "@/lib/validations/auth";


export default function ResetPasswordPage() {
  return (
    <Suspense>
      <ResetPasswordContent />
    </Suspense>
  );
}

function ResetPasswordContent() {
  const t = useTranslations("auth");
  const tErrors = useTranslations("errors");
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const [success, setSuccess] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
  });

  async function onSubmit(data: ResetPasswordFormValues) {
    if (!token) return;

    setIsLoading(true);
    setApiError(null);
    try {
      await resetPassword({ token, newPassword: data.newPassword });
      setSuccess(true);
    } catch (err: unknown) {
      const key = getErrorMessageKey(err as ApiError);
      setApiError(tErrors(key.replace("errors.", "")));
    } finally {
      setIsLoading(false);
    }
  }

  // Missing token — show error state
  if (!token) {
    return (
      <div className="mx-auto flex w-full max-w-sm flex-col items-center gap-6 py-12">
        <h1 className="text-2xl font-semibold tracking-tight">
          {t("resetPasswordTitle")}
        </h1>
        <p className="text-center text-sm text-destructive">
          {t("verifyEmailMissingToken")}
        </p>
        <Link
          href="/login"
          className="text-sm font-medium text-primary underline-offset-4 hover:underline"
        >
          {t("backToLogin")}
        </Link>
      </div>
    );
  }

  // Success state
  if (success) {
    return (
      <div className="mx-auto flex w-full max-w-sm flex-col items-center gap-6 py-12">
        <h1 className="text-2xl font-semibold tracking-tight">
          {t("resetPasswordTitle")}
        </h1>
        <p className="text-center text-sm text-muted-foreground">
          {t("resetPasswordSuccess")}
        </p>
        <Link
          href="/login"
          className="text-sm font-medium text-primary underline-offset-4 hover:underline"
        >
          {t("backToLogin")}
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-sm flex-col gap-6 py-12">
      <div className="flex flex-col gap-2 text-center">
        <h1 className="text-2xl font-semibold tracking-tight">
          {t("resetPasswordTitle")}
        </h1>
        <p className="text-sm text-muted-foreground">
          {t("resetPasswordDescription")}
        </p>
      </div>

      <form noValidate onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        {apiError && (
          <p className="text-center text-sm text-destructive" role="alert">
            {apiError}
          </p>
        )}

        <div className="flex flex-col gap-1.5">
          <FloatingPasswordInput
            id="newPassword"
            label={t("newPasswordLabel")}
            autoComplete="new-password"
            disabled={isLoading}
            showPasswordLabel={t("showPassword")}
            hidePasswordLabel={t("hidePassword")}
            className={cn(
              errors.newPassword && "border-destructive focus-visible:ring-destructive/20 focus-visible:border-destructive"
            )}
            {...register("newPassword")}
          />
          {errors.newPassword && (
            <p className="text-xs text-destructive" role="alert">
              {errors.newPassword.message}
            </p>
          )}
        </div>

        <div className="flex flex-col gap-1.5">
          <FloatingPasswordInput
            id="confirmPassword"
            label={t("confirmPasswordLabel")}
            autoComplete="new-password"
            disabled={isLoading}
            showPasswordLabel={t("showPassword")}
            hidePasswordLabel={t("hidePassword")}
            className={cn(
              errors.confirmPassword && "border-destructive focus-visible:ring-destructive/20 focus-visible:border-destructive"
            )}
            {...register("confirmPassword")}
          />
          {errors.confirmPassword && (
            <p className="text-xs text-destructive" role="alert">
              {errors.confirmPassword.message}
            </p>
          )}
        </div>

        <Button type="submit" disabled={isLoading} className="w-full">
          {isLoading ? t("submitting") : t("resetPasswordSubmit")}
        </Button>
      </form>

      <div className="text-center">
        <Link
          href="/login"
          className="text-sm font-medium text-primary underline-offset-4 hover:underline"
        >
          {t("backToLogin")}
        </Link>
      </div>
    </div>
  );
}
