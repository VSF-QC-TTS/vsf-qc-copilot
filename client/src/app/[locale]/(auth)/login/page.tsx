"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";

import { Button } from "@/components/ui/button";
import { FloatingInput, FloatingPasswordInput } from "@/components/ui/floating-input";
import { loginUser } from "@/lib/api/auth";
import type { ApiError } from "@/lib/api/types";
import { useAuthStore } from "@/lib/store/auth-store";
import { cn } from "@/lib/utils";
import { getErrorMessageKey } from "@/lib/utils/error-messages";
import { loginSchema, type LoginFormValues } from "@/lib/validations/auth";
import { Link } from "@/i18n/navigation";

// ---------------------------------------------------------------------------
// OAuth provider config
// ---------------------------------------------------------------------------

const OAUTH_PROVIDERS = [
  {
    id: "google",
    href: "/api/v1/oauth2/authorization/google",
    labelKey: "continueWithGoogle" as const,
    icon: (
      <svg viewBox="0 0 24 24" className="h-5 w-5" aria-hidden="true">
        <path
          d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z"
          fill="#4285F4"
        />
        <path
          d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
          fill="#34A853"
        />
        <path
          d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
          fill="#FBBC05"
        />
        <path
          d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
          fill="#EA4335"
        />
      </svg>
    ),
  },
  {
    id: "github",
    href: "/api/v1/oauth2/authorization/github",
    labelKey: "continueWithGithub" as const,
    icon: (
      <svg viewBox="0 0 24 24" className="h-5 w-5 fill-current" aria-hidden="true">
        <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12" />
      </svg>
    ),
  },
];

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

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

      {/* ----------------------------------------------------------------- */}
      {/* OAuth Buttons                                                      */}
      {/* ----------------------------------------------------------------- */}
      <div className="flex flex-col gap-2.5">
        {OAUTH_PROVIDERS.map((provider) => (
          <a key={provider.id} href={provider.href}>
            <Button
              type="button"
              variant="outline"
              className="w-full gap-2.5 font-medium shadow-xs transition-all duration-200 hover:bg-accent/60"
              asChild
            >
              <span>
                {provider.icon}
                {t(provider.labelKey)}
              </span>
            </Button>
          </a>
        ))}
      </div>

      {/* ----------------------------------------------------------------- */}
      {/* Divider                                                            */}
      {/* ----------------------------------------------------------------- */}
      <div className="relative flex items-center">
        <div className="flex-1 border-t border-border/60" />
        <span className="mx-3 text-xs font-medium uppercase tracking-wider text-muted-foreground/70">
          {t("orDivider")}
        </span>
        <div className="flex-1 border-t border-border/60" />
      </div>

      {/* ----------------------------------------------------------------- */}
      {/* Email / Password Form                                              */}
      {/* ----------------------------------------------------------------- */}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {serverError && (
          <div className="rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive dark:bg-destructive/10 animate-in fade-in duration-300">
            {serverError}
          </div>
        )}

        {/* Email field */}
        <div className="space-y-1.5">
          <FloatingInput
            id="email"
            type="email"
            label={t("email")}
            autoComplete="email"
            disabled={isSubmitting}
            className={cn(
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
          <FloatingPasswordInput
            id="password"
            label={t("password")}
            autoComplete="current-password"
            disabled={isSubmitting}
            showPasswordLabel={t("showPassword")}
            hidePasswordLabel={t("hidePassword")}
            className={cn(
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
          className="text-sm font-medium text-muted-foreground underline-offset-4 hover:text-primary transition-colors hover:underline"
        >
          {t("forgotPassword")}
        </Link>
        <p className="text-sm text-muted-foreground">
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
