import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${process.env.NEXT_PUBLIC_API_URL}/api/:path*`,
      },
    ];
  },
  images: {
    remotePatterns: [
      { protocol: "https", hostname: "avatars.githubusercontent.com" },
      { protocol: "https", hostname: "secure.gravatar.com" },
      { protocol: "https", hostname: "gitlab.com" },
      { protocol: "https", hostname: "*.stackexchange.com" },
      { protocol: "https", hostname: "i.stack.imgur.com" },
      { protocol: "https", hostname: "bitbucket.org" },
    ],
  },
};

export default nextConfig;
