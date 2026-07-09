'use client';
import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { useSession } from '@/store/session';

/** 로그인 없이 열 수 있는 경로. 하위 경로(/reset/request 등)도 함께 허용된다. */
const PUBLIC_ROUTES = ['/login', '/signup', '/reset', '/oauth'];

function isPublic(pathname: string) {
  return PUBLIC_ROUTES.some((p) => pathname === p || pathname.startsWith(`${p}/`));
}

/**
 * 클라이언트 라우트 가드 — 토큰이 localStorage(zustand persist)에 있어
 * 서버 미들웨어로는 로그인 여부를 알 수 없으므로 여기서 막는다.
 *
 * 첫 렌더에는 판단을 보류한다. 서버 렌더에는 스토리지가 없어 token이 항상 null이라,
 * 곧바로 검사하면 (1) 로그인 상태에서도 /login으로 튕기고 (2) 서버/클라이언트
 * 마크업이 어긋난다. localStorage는 동기라 모듈 로드 시점에 복원이 끝나므로,
 * 마운트 이후의 token은 이미 신뢰할 수 있는 값이다.
 */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const token = useSession((s) => s.token);
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);

  const allowed = isPublic(pathname) || !!token;

  useEffect(() => {
    if (!mounted || allowed) return;
    const next = encodeURIComponent(`${pathname}${window.location.search}`);
    router.replace(`/login?next=${next}`);
  }, [mounted, allowed, pathname, router]);

  // 복원 대기 중이거나 리다이렉트가 걸리는 동안 보호 페이지를 잠시라도 노출하지 않는다.
  if (!mounted || !allowed) return <div className="min-h-screen bg-background" />;
  return <>{children}</>;
}
