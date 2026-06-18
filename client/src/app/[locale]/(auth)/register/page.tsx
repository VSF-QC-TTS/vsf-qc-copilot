"use client";

import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { CheckIcon } from "@phosphor-icons/react";

import { Button } from "@/components/ui/button";
import { PasswordInput } from "@/components/ui/password-input";
import { Link } from "@/i18n/navigation";
import { registerUser } from "@/lib/api/auth";
import type { ApiError } from "@/lib/api/types";
import { cn } from "@/lib/utils";
import { getErrorMessageKey } from "@/lib/utils/error-messages";
import {
  registerSchema,
  type RegisterFormValues,
} from "@/lib/validations/auth";

const inputClassName =
  "flex h-10 w-full rounded-lg border border-border/80 bg-background/50 px-3 py-2 text-sm transition-all duration-200 placeholder:text-muted-foreground hover:border-primary/30 focus-visible:border-primary/50 focus-visible:bg-background focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-primary/10 disabled:cursor-not-allowed disabled:opacity-50 shadow-xs";

export default function RegisterPage() {
  const t = useTranslations("auth");
  const tErrors = useTranslations("errors");
  const [isSuccess, setIsSuccess] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [isPasswordFocused, setIsPasswordFocused] = useState(false);

  const {
    register,
    handleSubmit,
    control,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      fullName: "",
      email: "",
      password: "",
      confirmPassword: "",
    },
  });

  const password = useWatch({
    control,
    name: "password",
    defaultValue: "",
  });

  const requirements = [
    { met: password.length >= 8, label: t("passwordMinLength") },
    { met: /[A-Z]/.test(password), label: t("passwordUppercase") },
    { met: /[a-z]/.test(password), label: t("passwordLowercase") },
    { met: /[0-9]/.test(password), label: t("passwordNumber") },
  ];
  const shouldShowPasswordRequirements =
    isPasswordFocused || password.length > 0 || Boolean(errors.password);
  const passwordField = register("password");

  async function onSubmit(values: RegisterFormValues) {
    setServerError(null);

    try {
      await registerUser({
        email: values.email,
        password: values.password,
        fullName: values.fullName,
      });
      setIsSuccess(true);
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

  if (isSuccess) {
    const email = getValues("email");
    const domain = email.split("@")[1] || "";
    let mailUrl = `https://${domain}`;
    
    if (domain === "gmail.com") {
      mailUrl = "https://mail.google.com/";
    } else if (domain.includes("yahoo")) {
      mailUrl = "https://mail.yahoo.com/";
    } else if (domain === "outlook.com" || domain === "hotmail.com") {
      mailUrl = "https://outlook.live.com/";
    }

    return (
      <div className="flex flex-col items-center gap-4 text-center py-4 animate-in zoom-in-95 duration-400">
        <div className="flex h-14 w-14 items-center justify-center rounded-full bg-green-500/10 border border-green-500/20 text-green-600 dark:text-green-400 shadow-xs">
          <CheckIcon size={28} weight="bold" />
        </div>
        <div className="space-y-1.5">
          <h3 className="text-lg font-bold text-foreground">Đăng ký tài khoản thành công</h3>
          <p className="text-sm text-muted-foreground max-w-[280px]">{t("registerSuccess")}</p>
        </div>
        <div className="flex flex-col sm:flex-row gap-3 mt-2 w-full max-w-[300px] justify-center">
          <Button asChild variant="outline" className="transition-transform active:scale-[0.98] w-full sm:flex-1">
            <Link href="/login">
              {t("goToLogin")}
            </Link>
          </Button>
          <Button asChild className="transition-transform active:scale-[0.98] w-full sm:flex-1">
            <a href={mailUrl} target="_blank" rel="noopener noreferrer">
              Kiểm tra email
            </a>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="space-y-1.5 text-center">
        <h2 className="text-xl font-bold tracking-tight text-foreground">
          {t("registerTitle")}
        </h2>
        <p className="text-sm text-muted-foreground">
          {t("registerDescription")}
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {serverError && (
          <div className="rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive dark:bg-destructive/10 animate-in fade-in duration-300">
            {serverError}
          </div>
        )}

        {/* Full Name */}
        <div className="space-y-1.5">
          <label
            htmlFor="fullName"
            className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/90"
          >
            {t("fullNameLabel")}
          </label>
          <input
            id="fullName"
            type="text"
            autoComplete="name"
            placeholder={t("fullNamePlaceholder")}
            disabled={isSubmitting}
            className={cn(
              inputClassName,
              errors.fullName &&
                "border-destructive focus-visible:ring-destructive/20 focus-visible:border-destructive",
            )}
            {...register("fullName")}
          />
          {errors.fullName && (
            <p className="text-xs font-medium text-destructive mt-1">
              {errors.fullName.message}
            </p>
          )}
        </div>

        {/* Email */}
        <div className="space-y-1.5">
          <label
            htmlFor="email"
            className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/90"
          >
            {t("emailLabel")}
          </label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            placeholder={t("emailPlaceholder")}
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

        {/* Password */}
        <div className="space-y-1.5">
          <label
            htmlFor="password"
            className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/90"
          >
            {t("passwordLabel")}
          </label>
          <PasswordInput
            id="password"
            autoComplete="new-password"
            placeholder={t("passwordPlaceholder")}
            disabled={isSubmitting}
            showPasswordLabel={t("showPassword")}
            hidePasswordLabel={t("hidePassword")}
            className={cn(
              inputClassName,
              errors.password &&
                "border-destructive focus-visible:ring-destructive/20 focus-visible:border-destructive",
            )}
            {...passwordField}
            onFocus={() => setIsPasswordFocused(true)}
            onBlur={(event) => {
              passwordField.onBlur(event);
              setIsPasswordFocused(false);
            }}
          />
          {errors.password && (
            <p className="text-xs font-medium text-destructive mt-1">
              {errors.password.message}
            </p>
          )}

          {/* Password strength indicator */}
          <div
            aria-hidden={!shouldShowPasswordRequirements}
            className={cn(
              "overflow-hidden transition-[max-height,opacity,padding] duration-300 ease-out",
              shouldShowPasswordRequirements
                ? "max-h-[160px] pt-2 pb-1 opacity-100"
                : "max-h-0 pt-0 pb-0 opacity-0",
            )}
          >
            <div className="rounded-lg border bg-muted/20 p-3 space-y-2 dark:bg-muted/5">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground/85">
                Yêu cầu mật khẩu
              </p>
              <ul className="grid grid-cols-2 gap-2 text-xs">
                {requirements.map((req) => (
                  <li key={req.label} className="flex items-center gap-2">
                    <span className={cn(
                      "flex h-4 w-4 shrink-0 items-center justify-center rounded-full border transition-all duration-300",
                      req.met 
                        ? "bg-green-500/10 border-green-500/30 text-green-600 dark:text-green-400" 
                        : "bg-muted/50 border-border text-muted-foreground/60"
                    )}>
                      {req.met ? (
                        <CheckIcon size={10} weight="bold" />
                      ) : (
                        <div className="h-1 w-1 rounded-full bg-muted-foreground/40" />
                      )}
                    </span>
                    <span
                      className={cn(
                        "transition-colors duration-300 font-medium text-[11px]",
                        req.met ? "text-green-600 dark:text-green-400" : "text-muted-foreground"
                      )}
                    >
                      {req.label}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>

        {/* Confirm Password */}
        <div className="space-y-1.5">
          <label
            htmlFor="confirmPassword"
            className="text-xs font-semibold uppercase tracking-wider text-muted-foreground/90"
          >
            {t("confirmPasswordLabel")}
          </label>
          <PasswordInput
            id="confirmPassword"
            autoComplete="new-password"
            placeholder={t("confirmPasswordPlaceholder")}
            disabled={isSubmitting}
            showPasswordLabel={t("showPassword")}
            hidePasswordLabel={t("hidePassword")}
            className={cn(
              inputClassName,
              errors.confirmPassword &&
                "border-destructive focus-visible:ring-destructive/20 focus-visible:border-destructive",
            )}
            {...register("confirmPassword")}
          />
          {errors.confirmPassword && (
            <p className="text-xs font-medium text-destructive mt-1">
              {errors.confirmPassword.message}
            </p>
          )}
        </div>

        {/* Submit button */}
        <Button 
          type="submit" 
          className="w-full transition-transform active:scale-[0.98] font-medium shadow-xs" 
          disabled={isSubmitting}
        >
          {isSubmitting ? t("registerSubmitting") : t("registerButton")}
        </Button>
      </form>

      {/* Bottom link */}
      <div className="flex flex-col items-center gap-2 text-sm pt-2">
        <p className="text-sm text-muted-foreground">
          {t("alreadyHaveAccount")}{" "}
          <Link
            href="/login"
            className="font-semibold text-foreground underline-offset-4 hover:text-primary transition-colors hover:underline"
          >
            {t("loginLink")}
          </Link>
        </p>
      </div>
    </div>
  );
}
