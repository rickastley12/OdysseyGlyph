import type { Metadata } from "next";
import { Inter } from "next/font/google";
import localFont from "next/font/local";
import "./globals.css";

const inter = Inter({
  variable: "--font-sans",
  subsets: ["latin"],
});

const ndot = localFont({
  src: "../public/fonts/Ndot57-Regular.otf",
  variable: "--font-ndot",
  weight: "400",
});

export const metadata: Metadata = {
  title: "Glyph Odyssey",
  description: "Unleash the Glyph. True audio-visual synergy.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${inter.variable} ${ndot.variable}`}>
      <body>{children}</body>
    </html>
  );
}
