import type { Metadata } from "next";
import { Sidebar } from "@/components/sidebar";
import "./globals.css";

export const metadata: Metadata = {
  title: "TopicScanner",
  description: "DevRel Intelligence Platform",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="font-sans antialiased">
        <Sidebar />
        <main className="lg:pl-56 min-h-screen bg-gray-50/50">
          <div className="mx-auto max-w-7xl px-4 pt-14 pb-6 lg:px-6 lg:pt-6">{children}</div>
        </main>
      </body>
    </html>
  );
}
