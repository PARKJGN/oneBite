'use client';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useSession } from '@/store/session';

// 로그인 없이 접근 가능한 라우트.
// /onboard 는 가입·소셜 로그인 직후(세션 보유) 에만 도달하므로 보호 대상에 둔다.
const PUBLIC = ['/login', '/signup', '/oauth', '/reset', '/privacy', '/terms', '/account-deletion'];

const isPublicPath = (path: string) =>
  PUBLIC.some((p) => path === p || path.startsWith(p + '/'));

// zustand persist 가 localStorage 를 복원(hydrate) 했는지 추적.
// 복원 전엔 userId 가 항상 null 이라, 이 시점에 가드를 적용하면 로그인 사용자가 오튕긴다.
function useHydrated() {
  const [hydrated, setHydrated] = useState(false);
  useEffect(() => {
    setHydrated(useSession.persist.hasHydrated());
    return useSession.persist.onFinishHydration(() => setHydrated(true));
  }, []);
  return hydrated;
}

/**
 * 클라이언트 인증 가드. 세션이 없고 보호 라우트면 /login 으로 보낸다.
 * 미인증 보호 라우트에서는 본문을 렌더하지 않아 깜빡임(플래시)도 막는다.
 * 서버·복원 전 첫 렌더는 모두 null 이라 하이드레이션 불일치가 없다.
 */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const path = usePathname();
  const router = useRouter();
  const hydrated = useHydrated();
  const userId = useSession((s) => s.userId);
  const publicRoute = isPublicPath(path);

  useEffect(() => {
    if (hydrated && !publicRoute && userId == null) router.replace('/login');
  }, [hydrated, publicRoute, userId, path, router]);

  // 공개 라우트는 항상 렌더. 보호 라우트는 세션 확인(복원 완료 + userId 존재) 후에만 렌더.
  if (!publicRoute && (!hydrated || userId == null)) return null;
  return <>{children}</>;
}
