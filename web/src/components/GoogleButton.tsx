'use client';
import { useGoogleLogin } from '@react-oauth/google';

/**
 * Google 웹 로그인 버튼 — implicit flow로 access_token을 클라이언트에서 받아 onToken으로 전달.
 * 이 토큰을 백엔드 /auth/social 로 보내면 userinfo로 검증된다.
 * (GoogleOAuthProvider 안에서만 렌더 — providers.tsx가 client ID 있을 때만 감쌈)
 */
export function GoogleButton({ onToken, disabled }: { onToken: (accessToken: string) => void; disabled?: boolean }) {
  const login = useGoogleLogin({
    flow: 'implicit',
    onSuccess: (resp) => onToken(resp.access_token),
    onError: () => alert('Google 로그인에 실패했어요.'),
  });
  return (
    <button onClick={() => login()} disabled={disabled}
      className="h-12 rounded-md border border-input bg-white text-sm font-semibold text-ink">
      Google로 계속하기
    </button>
  );
}
