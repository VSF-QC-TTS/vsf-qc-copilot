'use client';

import { useTheme } from 'next-themes';
import { useEffect, useState } from 'react';
import { Sun, Moon, Desktop } from '@phosphor-icons/react';
import { Button } from '@/components/ui/button';

export function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  if (!mounted)
    return (
      <Button variant="ghost" size="icon" disabled>
        <Sun size={18} />
      </Button>
    );

  const cycleTheme = () => {
    if (theme === 'light') setTheme('dark');
    else if (theme === 'dark') setTheme('system');
    else setTheme('light');
  };

  const Icon = theme === 'dark' ? Moon : theme === 'system' ? Desktop : Sun;
  const label =
    theme === 'dark'
      ? 'Dark mode'
      : theme === 'system'
        ? 'System theme'
        : 'Light mode';

  return (
    <Button variant="ghost" size="icon" onClick={cycleTheme} aria-label={label}>
      <Icon size={18} />
      <span className="sr-only">{label}</span>
    </Button>
  );
}
