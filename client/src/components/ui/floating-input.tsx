"use client";

import * as React from "react";
import { EyeIcon, EyeSlashIcon } from "@phosphor-icons/react";
import { cn } from "@/lib/utils";

interface FloatingInputProps extends React.ComponentProps<"input"> {
  label: string;
}

export const FloatingInput = React.forwardRef<HTMLInputElement, FloatingInputProps>(
  ({ className, label, id, ...props }, ref) => {
    return (
      <div className="relative">
        <input
          id={id}
          ref={ref}
          className={cn(
            "peer flex h-14 w-full rounded-lg border border-border/80 bg-background/50 px-3 pb-2 pt-6 text-sm transition-all duration-200 placeholder-transparent hover:border-primary/30 focus-visible:border-primary/50 focus-visible:bg-background focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-primary/10 disabled:cursor-not-allowed disabled:opacity-50 shadow-xs",
            className
          )}
          placeholder={label}
          {...props}
        />
        <label
          htmlFor={id}
          className="pointer-events-none absolute left-3 top-2.5 text-[10px] uppercase tracking-wider font-semibold text-muted-foreground/70 transition-all peer-placeholder-shown:top-[17px] peer-placeholder-shown:text-sm peer-placeholder-shown:normal-case peer-placeholder-shown:tracking-normal peer-focus:top-2.5 peer-focus:text-[10px] peer-focus:uppercase peer-focus:tracking-wider peer-focus:text-primary"
        >
          {label}
        </label>
      </div>
    );
  }
);
FloatingInput.displayName = "FloatingInput";

interface FloatingPasswordInputProps extends FloatingInputProps {
  showPasswordLabel?: string;
  hidePasswordLabel?: string;
}

export const FloatingPasswordInput = React.forwardRef<HTMLInputElement, FloatingPasswordInputProps>(
  ({ className, label, id, showPasswordLabel = "Show password", hidePasswordLabel = "Hide password", disabled, ...props }, ref) => {
    const [isVisible, setIsVisible] = React.useState(false);
    const Icon = isVisible ? EyeSlashIcon : EyeIcon;
    const buttonLabel = isVisible ? hidePasswordLabel : showPasswordLabel;

    return (
      <div className="relative">
        <input
          id={id}
          type={isVisible ? "text" : "password"}
          ref={ref}
          disabled={disabled}
          className={cn(
            "peer flex h-14 w-full rounded-lg border border-border/80 bg-background/50 px-3 pb-2 pt-6 pr-11 text-sm transition-all duration-200 placeholder-transparent hover:border-primary/30 focus-visible:border-primary/50 focus-visible:bg-background focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-primary/10 disabled:cursor-not-allowed disabled:opacity-50 shadow-xs",
            className
          )}
          placeholder={label}
          {...props}
        />
        <label
          htmlFor={id}
          className="pointer-events-none absolute left-3 top-2.5 text-[10px] uppercase tracking-wider font-semibold text-muted-foreground/70 transition-all peer-placeholder-shown:top-[17px] peer-placeholder-shown:text-sm peer-placeholder-shown:normal-case peer-placeholder-shown:tracking-normal peer-focus:top-2.5 peer-focus:text-[10px] peer-focus:uppercase peer-focus:tracking-wider peer-focus:text-primary"
        >
          {label}
        </label>
        <button
          type="button"
          aria-label={buttonLabel}
          aria-pressed={isVisible}
          disabled={disabled}
          onMouseDown={(event) => event.preventDefault()}
          onClick={() => setIsVisible((current) => !current)}
          className="absolute right-1 top-1/2 inline-flex size-8 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
        >
          <Icon size={18} aria-hidden="true" />
        </button>
      </div>
    );
  }
);
FloatingPasswordInput.displayName = "FloatingPasswordInput";
