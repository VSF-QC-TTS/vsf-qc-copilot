import { redirect } from "next/navigation";
import { defaultLocale } from "@/i18n/config";

/**
 * Catch-all root — redirects to the default locale.
 * In practice, next-intl middleware handles this before reaching the page,
 * but this acts as a safety net for direct / access.
 */
export default function RootPage() {
  redirect(`/${defaultLocale}`);
}
