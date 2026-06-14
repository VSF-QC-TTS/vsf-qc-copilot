import { redirect } from "next/navigation";

/**
 * Locale root — redirects to the dashboard.
 * The AuthGuard on the dashboard layout will redirect
 * unauthenticated users to the login page.
 */
export default function Home() {
  redirect("/dashboard");
}
