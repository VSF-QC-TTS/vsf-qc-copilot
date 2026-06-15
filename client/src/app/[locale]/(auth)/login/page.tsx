"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { PasswordInput } from "@/components/ui/password-input";
import { loginUser } from "@/lib/api/auth";
import type { ApiError } from "@/lib/api/types";
import { useAuthStore } from "@/lib/store/auth-store";
import { cn } from "@/lib/utils";
import { getErrorMessageKey } from "@/lib/utils/error-messages";
import { loginSchema, type LoginFormValues } from "@/lib/validations/auth";
import { Link } from "@/i18n/navigation";

const inputClassName =
  "flex h-10 w-full rounded-lg border border-border/80 bg-background/50 px-3 py-2 text-sm transition-all duration-200 placeholder:text-muted-foreground hover:border-primary/30 focus-visible:border-primary/50 focus-visible:bg-background focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-primary/10 disabled:cursor-not-allowed disabled:opacity-50 shadow-xs";

export default function LoginPage() {
  const t = useTranslations("auth");
  const tErrors = useTranslations("errors");
  const router = useRouter();

  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  async function onSubmit(values: LoginFormValues) {
    setServerError(null);

    try {
      const data = await loginUser(values);
      useAuthStore.getState().login(data.accessToken, data.user);
      router.push("/dashboard");
    } catch (error: unknown) {
      if (
        typeof error === "object" &&
        error !== null &&
        "code" in error &&
        "status" in error &&
        "message" in error
      ) {
        const apiError = error as ApiError;
        const messageKey = getErrorMessageKey(apiError);
        const key = messageKey.replace(/^errors\./, "");
        setServerError(tErrors(key));
      } else {
        setServerError(tErrors("network"));
      }
    }
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1.5 text-center">
        <h2 className="text-xl font-bold tracking-tight text-foreground">
          {t("loginTitle")}
        </h2>
        <p className="text-sm text-muted-foreground">{t("loginSubtitle")}</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {serverError && (
          <div className="rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive dark:bg-destructive/10 animate-in fade-in duration-300">
            {serverError}
          </div>
        )}

        {/* Email field */}
        <div className="space-y-1.5">
          <label
            htmlFor="email"
            className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/90"
          >
            {t("email")}
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            disabled={isSubmitting}
            className={cn(
              inputClassName,
              errors.email &&
                "border-destructive focus-visible:ring-destructive/20 focus-visible:border-destructive",
            )}
            {...register("email")}
          />
          {errors.email && (
            <p className="text-xs font-medium text-destructive mt-1">{errors.email.message}</p>
          )}
        </div>

        {/* Password field */}
        <div className="space-y-1.5">
          <label
            htmlFor="password"
            className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/90"
          >
            {t("password")}
          </label>
          <PasswordInput
            id="password"
            autoComplete="current-password"
            disabled={isSubmitting}
            showPasswordLabel={t("showPassword")}
            hidePasswordLabel={t("hidePassword")}
            className={cn(
              inputClassName,
              errors.password &&
                "border-destructive focus-visible:ring-destructive/20 focus-visible:border-destructive",
            )}
            {...register("password")}
          />
          {errors.password && (
            <p className="text-xs font-medium text-destructive mt-1 text-left">
              {errors.password.message}
            </p>
          )}
        </div>

        {/* Submit button */}
        <Button 
          type="submit" 
          className="w-full transition-transform active:scale-[0.98] font-medium shadow-xs" 
          disabled={isSubmitting}
        >
          {isSubmitting ? t("authenticating") : t("login")}
        </Button>
      </form>

      {/* Bottom links */}
      <div className="flex flex-col items-center gap-2.5 text-sm pt-2">
        <Link
          href="/forgot-password"
          className="text-xs font-medium text-muted-foreground underline-offset-4 hover:text-primary transition-colors hover:underline"
        >
          {t("forgotPassword")}
        </Link>
        <p className="text-xs text-muted-foreground">
          {t("noAccount")}{" "}
          <Link
            href="/register"
            className="font-semibold text-foreground underline-offset-4 hover:text-primary transition-colors hover:underline"
          >
            {t("register")}
          </Link>
        </p>
      </div>
    </div>
  );
}
