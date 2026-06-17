'use client';

import Image from 'next/image';
import { cn } from '@/lib/utils';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type AvatarSize = 'sm' | 'md' | 'lg';

interface UserAvatarProps {
  displayName: string;
  avatarUrl?: string | null;
  size?: AvatarSize;
  className?: string;
}

// ---------------------------------------------------------------------------
// Size config
// ---------------------------------------------------------------------------

const sizeMap: Record<AvatarSize, { container: string; text: string; px: number }> = {
  sm: { container: 'h-8 w-8', text: 'text-xs', px: 32 },
  md: { container: 'h-10 w-10', text: 'text-sm', px: 40 },
  lg: { container: 'h-24 w-24', text: 'text-2xl', px: 96 },
};

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function getInitials(name: string): string {
  return name
    .split(' ')
    .map((part) => part[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function UserAvatar({ displayName, avatarUrl, size = 'md', className }: UserAvatarProps) {
  const config = sizeMap[size];
  const initials = getInitials(displayName || '?');

  return (
    <div
      className={cn(
        'relative flex shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground font-semibold select-none overflow-hidden',
        config.container,
        config.text,
        className,
      )}
    >
      {avatarUrl ? (
        <Image
          src={avatarUrl}
          alt={displayName}
          width={config.px}
          height={config.px}
          className="h-full w-full object-cover"
        />
      ) : (
        initials
      )}
    </div>
  );
}
