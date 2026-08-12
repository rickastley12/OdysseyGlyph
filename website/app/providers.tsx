"use client";

import { MotionConfig } from "framer-motion";

export function Providers({ children }: { children: React.ReactNode }) {
  // In development, force animations to always run so we can test them.
  // In production, respect the user's OS-level reduced motion preference.
  return (
    <MotionConfig reducedMotion={process.env.NODE_ENV === "production" ? "user" : "never"}>
      {children}
    </MotionConfig>
  );
}
