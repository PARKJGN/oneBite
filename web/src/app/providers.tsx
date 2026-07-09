'use client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { GoogleOAuthProvider } from '@react-oauth/google';
import { useState } from 'react';
import { AuthGuard } from '@/components/AuthGuard';

const googleClientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;

export function Providers({ children }: { children: React.ReactNode }) {
  const [client] = useState(() => new QueryClient());
  const tree = (
    <QueryClientProvider client={client}>
      <AuthGuard>{children}</AuthGuard>
    </QueryClientProvider>
  );
  // Google Web 클라이언트 ID가 있을 때만 GoogleOAuthProvider로 감싼다(없으면 소셜은 dev 폴백).
  return googleClientId ? <GoogleOAuthProvider clientId={googleClientId}>{tree}</GoogleOAuthProvider> : tree;
}
