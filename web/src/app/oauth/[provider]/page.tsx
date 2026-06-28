'use client';
import { Suspense, useEffect, useRef } from 'react';
import { useParams, useRouter, useSearchParams } from 'next/navigation';
import { useSocialCodeLogin } from '@/lib/hooks';
import { useSession } from '@/store/session';

/**
 * 웹 소셜 OAuth 콜백 — 카카오/네이버 authorize 후 ?code(&state)로 돌아오는 지점.
 * code를 백엔드 /auth/social/code 로 보내 교환·검증 → JWT 저장 → 이동.
 */
function OAuthCallbackInner() {
  const router = useRouter();
  const provider = useParams<{ provider: string }>().provider;
  const sp = useSearchParams();
  const setSession = useSession((s) => s.setSession);
  const social = useSocialCodeLogin();
  const ran = useRef(false); // StrictMode 이중 실행 방지(인가코드는 1회용)

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;
    const code = sp.get('code');
    const state = sp.get('state') ?? undefined;
    if (sp.get('error') || !code) {
      alert('소셜 로그인이 취소되었어요.');
      router.replace('/login');
      return;
    }
    const redirectUri = `${window.location.origin}/oauth/${provider}`;
    social.mutate(
      { provider, code, redirectUri, state },
      {
        onSuccess: (r) => { setSession(r.userId, r.token, r.refreshToken); router.replace(r.isNew ? '/onboard' : '/'); },
        onError: () => { alert('소셜 로그인에 실패했어요.'); router.replace('/login'); },
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return <main className="flex min-h-screen items-center justify-center text-ink-soft">로그인 처리 중…</main>;
}

export default function OAuthCallback() {
  return (
    <Suspense fallback={null}>
      <OAuthCallbackInner />
    </Suspense>
  );
}
