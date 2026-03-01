import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";

const geistSans = localFont({
  src: "./fonts/GeistVF.woff",
  variable: "--font-geist-sans",
  weight: "100 900",
});
const geistMono = localFont({
  src: "./fonts/GeistMonoVF.woff",
  variable: "--font-geist-mono",
  weight: "100 900",
});

export const metadata: Metadata = {
  title: "SolanaSuper — The Decentralized Super App",
  description: "Your portal to the entire Solana network. Manage wealth, invest in DeFi, vote anonymously, and transact 100% offline. One app to rule them all.",
  keywords: ["Solana", "DeFi", "crypto", "wallet", "offline transactions", "Jupiter", "Jito", "Drift", "IPFS", "Arcium"],
  openGraph: {
    title: "SolanaSuper — The Decentralized Super App",
    description: "Manage wealth, invest in DeFi, vote anonymously, and transact 100% offline on Solana.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        {children}
      </body>
    </html>
  );
}
