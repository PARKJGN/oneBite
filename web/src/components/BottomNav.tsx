'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { HistoryIcon, HomeIcon, SlotsIcon } from './icons';
import { cn } from '@/lib/utils';

// 앱과 동일한 하단 알약형 탭바(오늘/히스토리/내 슬롯). 책갈피·설정은 헤더 햄버거.
// 디자인: 떠 있는 pill(h62, border-radius full, 그림자, space-around) — mobile BottomTabBar 와 동일.
const TABS = [
  { href: '/', label: '오늘', Icon: HomeIcon },
  { href: '/history', label: '히스토리', Icon: HistoryIcon },
  { href: '/slots', label: '내 슬롯', Icon: SlotsIcon },
];

export function BottomNav() {
  const path = usePathname();
  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 pt-2 pb-[max(14px,env(safe-area-inset-bottom))]">
      <div className="container max-w-2xl">
        <div className="flex h-[62px] items-center justify-around rounded-full border border-border bg-[#fdfcf7] px-2 shadow-[0_6px_20px_rgba(20,40,24,0.10)]">
          {TABS.map(({ href, label, Icon }) => {
            const active = href === '/' ? path === '/' : path.startsWith(href);
            return (
              <Link
                key={href}
                href={href}
                className={cn(
                  'flex flex-1 flex-col items-center justify-center gap-1 py-1.5',
                  active ? 'text-pine' : 'text-ink-faint',
                )}
              >
                <Icon size={22} />
                <span className="text-[10px] font-semibold">{label}</span>
              </Link>
            );
          })}
        </div>
      </div>
    </nav>
  );
}
