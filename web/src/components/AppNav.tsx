'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { MenuIcon } from './icons';
import { BottomNav } from './BottomNav';
import { useSession } from '@/store/session';

// 앱과 통일된 상단 헤더 — 좌측 oneBite 워드마크, 우측 햄버거 → 드롭다운(내 책갈피/설정/로그아웃).
// 하단은 BottomNav(오늘/히스토리/내 슬롯). 디자인 시스템(oneBite.dc.html) = 모바일 Header/BottomTabBar 기준.
export function AppNav() {
  const [open, setOpen] = useState(false);
  const router = useRouter();
  const go = (href: string) => {
    setOpen(false);
    router.push(href);
  };
  const logout = () => {
    setOpen(false);
    useSession.getState().clear();
    router.replace('/login');
  };

  return (
    <>
      <header className="sticky top-0 z-30 border-b border-border bg-paper">
        <div className="container flex h-12 max-w-2xl items-center justify-between">
          <Link href="/" className="font-serif text-lg font-bold text-pine">oneBite</Link>
          <div className="relative">
            <button onClick={() => setOpen((v) => !v)} aria-label="메뉴" className="rounded-md p-1 text-ink">
              <MenuIcon size={22} />
            </button>
            {open && (
              <>
                {/* 바깥 클릭 시 닫힘 */}
                <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} aria-hidden />
                <div className="absolute right-0 z-50 mt-2 w-44 rounded-[13px] border border-border bg-white p-1.5 shadow-[0_12px_30px_rgba(20,40,24,0.16)]">
                  <button onClick={() => go('/bookmarks')} className="block w-full rounded-[9px] px-3.5 py-2.5 text-left text-sm font-semibold text-ink hover:bg-cloud">내 책갈피</button>
                  <div className="my-1 h-px bg-border" />
                  <button onClick={() => go('/settings')} className="block w-full rounded-[9px] px-3.5 py-2.5 text-left text-sm font-semibold text-ink hover:bg-cloud">설정</button>
                  <div className="my-1 h-px bg-border" />
                  <button onClick={logout} className="block w-full rounded-[9px] px-3.5 py-2.5 text-left text-sm font-semibold text-danger hover:bg-cloud">로그아웃</button>
                </div>
              </>
            )}
          </div>
        </div>
      </header>
      <BottomNav />
    </>
  );
}
