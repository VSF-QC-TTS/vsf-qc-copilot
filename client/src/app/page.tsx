import { redirect } from "next/navigation";
/**
 * Catch-all root — redirects to the dashboard.
 * In practice, next-intl proxy handles this before reaching the page,
 * but this acts as a safety net for direct / access.
 */
export default function RootPage() {
  redirect("/dashboard");
}
