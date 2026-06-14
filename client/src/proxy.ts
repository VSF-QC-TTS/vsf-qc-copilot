import createMiddleware from "next-intl/middleware";
import { routing } from "./i18n/routing";

const proxy = createMiddleware(routing);

export default proxy;

export const config = {
  // Match all pathnames except API routes, static files, and Next.js internals.
  matcher: ["/((?!api|_next|.*\\..*).*)"],
};
