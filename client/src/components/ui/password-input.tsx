"use client";

import * as React from "react";
import { EyeIcon, EyeSlashIcon } from "@phosphor-icons/react";

import { cn } from "@/lib/utils";

interface PasswordInputProps extends Omit<
  React.ComponentProps<"input">,
  "type"
> {
  showPasswordLabel: string;
  hidePasswordLabel: string;
}

export function PasswordInput({
  className,
  disabled,
  showPasswordLabel,
  hidePasswordLabel,
  ...props
}: PasswordInputProps) {
  const [isVisible, setIsVisible] = React.useState(false);
  const label = isVisible ? hidePasswordLabel : showPasswordLabel;
  const Icon = isVisible ? EyeSlashIcon : EyeIcon;

  return (
    <div className="relative">
      <input
        type={isVisible ? "text" : "password"}
        disabled={disabled}
        className={cn(className, "pr-11")}
        {...props}
      />
      <button
        type="button"
        aria-label={label}
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
