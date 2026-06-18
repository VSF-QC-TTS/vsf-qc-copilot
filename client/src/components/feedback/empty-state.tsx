'use client';

import * as React from "react";
import { MagnifyingGlassIcon } from "@phosphor-icons/react";
import { motion, useReducedMotion, type Variants } from "motion/react";
import { cn } from "@/lib/utils";

export interface EmptyStateProps {
  /** Primary heading shown below the icon. */
  title: string;
  /** Optional secondary text below the title. */
  description?: string;
  /** Override the default search icon. */
  icon?: React.ReactNode;
  /** Optional CTA rendered below the description (e.g. a Button). */
  action?: React.ReactNode;
  className?: string;
}

export function EmptyState({
  title,
  description,
  icon,
  action,
  className,
}: EmptyStateProps) {
  const reduce = useReducedMotion();

  // Stagger variants
  const containerVariants: Variants = {
    hidden: { opacity: 0 },
    show: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1,
      },
    },
  };

  const itemVariants: Variants = {
    hidden: { opacity: 0, y: 12 },
    show: { 
      opacity: 1, 
      y: 0, 
      transition: { 
        type: "spring", 
        stiffness: 100, 
        damping: 15 
      } 
    },
  };

  return (
    <motion.div
      data-slot="empty-state"
      className={cn(
        "flex flex-col items-center justify-center gap-4 py-16 text-center rounded-2xl border border-dashed bg-muted/10",
        className,
      )}
      variants={reduce ? {} : containerVariants}
      initial="hidden"
      animate="show"
    >
      <motion.div 
        variants={reduce ? {} : itemVariants}
        className="flex h-20 w-20 items-center justify-center rounded-2xl bg-muted/30 text-muted-foreground/80 shadow-[inset_0_1px_0_rgba(255,255,255,0.05)] ring-1 ring-inset ring-muted"
      >
        {icon ?? <MagnifyingGlassIcon size={40} weight="duotone" />}
      </motion.div>

      <div className="space-y-1.5 flex flex-col items-center">
        <motion.h3 
          variants={reduce ? {} : itemVariants}
          className="text-base font-semibold tracking-tight text-foreground"
        >
          {title}
        </motion.h3>
        
        {description && (
          <motion.p 
            variants={reduce ? {} : itemVariants}
            className="text-sm text-muted-foreground max-w-[40ch] leading-relaxed"
          >
            {description}
          </motion.p>
        )}
      </div>

      {action && (
        <motion.div 
          variants={reduce ? {} : itemVariants}
          className="mt-3"
        >
          {action}
        </motion.div>
      )}
    </motion.div>
  );
}
