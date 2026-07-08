import type { Metadata } from 'next';
import './globals.css';
import { Providers } from './providers';
import { AppChrome } from '@/components/AppChrome';

export const metadata: Metadata = {
  title: 'oneBite',
  description: '하루 한 입, 깊이 있는 뉴스',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <Providers>
          <AppChrome />
          {children}
        </Providers>
      </body>
    </html>
  );
}
