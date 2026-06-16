import createNextIntlPlugin from "next-intl/plugin";
import type { NextConfig } from "next";

const withNextIntl = createNextIntlPlugin("./src/i18n/request.ts");

const nextConfig: NextConfig = {
  output: 'standalone',
  async rewrites() {
    const backendUrl = process.env.BACKEND_API_URL?.replace(/\/$/, '');
    if (backendUrl) {
      return {
        beforeFiles: [
          {
            source: '/api/v1/:path*',
            destination: `${backendUrl}/api/v1/:path*`,
          },
          {
            source: '/mock-chatbot/:path*',
            destination: `${backendUrl}/mock-chatbot/:path*`,
          },
        ],
      };
    }
    return [];
  },
};

export default withNextIntl(nextConfig);
