"use client";

import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { Check, X } from "@phosphor-icons/react";

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
  "flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50";

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
    return (
      <div className="flex flex-col items-center gap-4 text-center">
        <Check size={48} weight="bold" className="text-primary" />
        <p className="text-lg font-medium">{t("registerSuccess")}</p>
        <Link
          href="/login"
          className="text-sm font-medium text-primary underline-offset-4 hover:underline"
        >
          {t("goToLogin")}
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="space-y-2 text-center">
        <h2 className="text-lg font-semibold text-foreground">
          {t("registerTitle")}
        </h2>
        <p className="text-sm text-muted-foreground">
          {t("registerDescription")}
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {serverError && (
          <div className="rounded-md border border-destructive/50 bg-destructive/10 px-4 py-3 text-sm text-destructive">
            {serverError}
          </div>
        )}

        {/* Full Name */}
        <div className="space-y-2">
          <label
            htmlFor="fullName"
            className="text-sm font-medium leading-none text-foreground"
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
                "border-destructive focus-visible:ring-destructive",
            )}
            {...register("fullName")}
          />
          {errors.fullName && (
            <p className="text-sm text-destructive">
              {errors.fullName.message}
            </p>
          )}
        </div>

        {/* Email */}
        <div className="space-y-2">
          <label
            htmlFor="email"
            className="text-sm font-medium leading-none text-foreground"
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
                "border-destructive focus-visible:ring-destructive",
            )}
            {...register("email")}
          />
          {errors.email && (
            <p className="text-sm text-destructive">{errors.email.message}</p>
          )}
        </div>

        {/* Password */}
        <div className="space-y-2">
          <label
            htmlFor="password"
            className="text-sm font-medium leading-none text-foreground"
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
                "border-destructive focus-visible:ring-destructive",
            )}
            {...passwordField}
            onFocus={() => setIsPasswordFocused(true)}
            onBlur={(event) => {
              passwordField.onBlur(event);
              setIsPasswordFocused(false);
            }}
          />
          {errors.password && (
            <p className="text-sm text-destructive">
              {errors.password.message}
            </p>
          )}

          {/* Password strength indicator */}
          <div
            aria-hidden={!shouldShowPasswordRequirements}
            className={cn(
              "overflow-hidden transition-[max-height,opacity,padding] duration-200 ease-out",
              shouldShowPasswordRequirements
                ? "max-h-32 pt-1 opacity-100"
                : "max-h-0 pt-0 opacity-0",
            )}
          >
            <ul className="flex flex-col gap-1">
              {requirements.map((req) => (
                <li key={req.label} className="flex items-center gap-2 text-sm">
                  {req.met ? (
                    <Check size={16} weight="bold" className="text-green-600" />
                  ) : (
                    <X
                      size={16}
                      weight="bold"
                      className="text-muted-foreground"
                    />
                  )}
                  <span
                    className={cn(
                      req.met ? "text-green-600" : "text-muted-foreground",
                    )}
                  >
                    {req.label}
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* Confirm Password */}
        <div className="space-y-2">
          <label
            htmlFor="confirmPassword"
            className="text-sm font-medium leading-none text-foreground"
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
                "border-destructive focus-visible:ring-destructive",
            )}
            {...register("confirmPassword")}
          />
          {errors.confirmPassword && (
            <p className="text-sm text-destructive">
              {errors.confirmPassword.message}
            </p>
          )}
        </div>

        {/* Submit button */}
        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting ? t("registerSubmitting") : t("registerButton")}
        </Button>
      </form>

      {/* Bottom link */}
      <div className="flex flex-col items-center gap-2 text-sm">
        <p className="text-muted-foreground">
          {t("alreadyHaveAccount")}{" "}
          <Link
            href="/login"
            className="font-medium text-foreground underline-offset-4 hover:underline"
          >
            {t("loginLink")}
          </Link>
        </p>
      </div>
    </div>
  );
}
