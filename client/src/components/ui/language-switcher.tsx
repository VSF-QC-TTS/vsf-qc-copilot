"use client";

import { useLocale } from "next-intl";

import { useRouter as useNextRouter } from "next/navigation";
import { TranslateIcon } from "@phosphor-icons/react";
import { Button } from "@/components/ui/button";
import type { Locale } from "@/i18n/config";
import { locales } from "@/i18n/config";

const localeLabels: Record<Locale, string> = {
  vi: "Tiếng Việt",
  en: "English",
};

export function LanguageSwitcher() {
  const locale = useLocale() as Locale;
  const nextRouter = useNextRouter();

  const nextLocale = locales.find((l) => l !== locale) ?? locales[0];

  function handleSwitch() {
    document.cookie = `NEXT_LOCALE=${nextLocale}; path=/; max-age=31536000`;
    nextRouter.refresh();
  }

  return (
    <Button
      variant="ghost"
      size="sm"
      onClick={handleSwitch}
      className="gap-1.5 px-2"
      aria-label={`Switch to ${localeLabels[nextLocale]}`}
    >
      <TranslateIcon size={18} />
      <span className="text-xs font-semibold">{localeLabels[locale]}</span>
    </Button>
  );
}
