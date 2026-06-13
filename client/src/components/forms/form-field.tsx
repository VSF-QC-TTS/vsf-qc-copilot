import * as React from "react";

import { cn } from "@/lib/utils";

export interface FormFieldProps {
  /** Visible label text. */
  label: string;
  /** Must match the input's `id` attribute for label association. */
  htmlFor: string;
  /** Validation error message — replaces helper text when present. */
  error?: string;
  /** Hint text shown below the input when there is no error. */
  helperText?: string;
  /** Show a red asterisk after the label. */
  required?: boolean;
  children: React.ReactNode;
  className?: string;
}

export function FormField({
  label,
  htmlFor,
  error,
  helperText,
  required = false,
  children,
  className,
}: FormFieldProps) {
  const errorId = `${htmlFor}-error`;
  const helperId = `${htmlFor}-helper`;

  /** The id of the currently visible description element. */
  const describedBy = error ? errorId : helperText ? helperId : undefined;

  return (
    <div data-slot="form-field" className={cn("space-y-1.5", className)}>
      {/* Label */}
      <label
        htmlFor={htmlFor}
        className="text-sm font-medium leading-none text-foreground"
      >
        {label}
        {required && (
          <span className="ml-0.5 text-destructive" aria-hidden="true">
            *
          </span>
        )}
      </label>

      {/* Input slot — inject aria-describedby */}
      {React.isValidElement<Record<string, unknown>>(children)
        ? React.cloneElement(children, {
            "aria-describedby": describedBy,
            "aria-invalid": error ? true : undefined,
          })
        : children}

      {/* Error text (takes priority over helper text) */}
      {error && (
        <p id={errorId} className="text-xs text-destructive" role="alert">
          {error}
        </p>
      )}

      {/* Helper text */}
      {!error && helperText && (
        <p id={helperId} className="text-xs text-muted-foreground">
          {helperText}
        </p>
      )}
    </div>
  );
}
