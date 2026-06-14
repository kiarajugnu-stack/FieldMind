import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "FieldMinds - Structured Observation & Research Platform",
  description: "Track observations, document research sessions, and analyze patterns in your field work with FieldMinds.",
  openGraph: {
    title: "FieldMinds",
    description: "Track observations, document research sessions, and analyze patterns",
    url: "https://fieldminds.app",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="scroll-smooth">
      <body className="bg-gradient-to-br from-background to-primary-light/10 text-on-background">
        {children}
      </body>
    </html>
  );
}
