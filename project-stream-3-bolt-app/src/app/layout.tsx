import './globals.css';
import { Inter } from 'next/font/google';

const inter = Inter({ subsets: ['latin'] });

export const metadata = {
  title: 'Online Store - SSR Example',
  description: 'A server-side rendered online store example',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <nav className="bg-gray-800 text-white p-4">
          <div className="container mx-auto flex justify-between items-center">
            <a href="/" className="text-xl font-bold">Online Store</a>
            <div className="space-x-4">
              <a href="/products" className="hover:text-gray-300">Products</a>
              <a href="/cart" className="hover:text-gray-300">Cart</a>
              <a href="/account" className="hover:text-gray-300">Account</a>
            </div>
          </div>
        </nav>
        <main className="container mx-auto px-4 py-8">
          {children}
        </main>
        <footer className="bg-gray-100 border-t">
          <div className="container mx-auto p-4 text-center text-gray-600">
            © 2023 Online Store. All rights reserved.
          </div>
        </footer>
      </body>
    </html>
  );
}