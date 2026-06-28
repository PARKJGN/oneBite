'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';

const TABS = [
  { href: '/', label: '오늘' },
  { href: '/history', label: '히스토리' },
  { href: '/bookmarks', label: '책갈피' },
  { href: '/slots', label: '내 슬롯' },
  { href: '/settings', label: '설정' },
];

export function AppNav() {
  const path = usePathname();
  return (
    <header className="border-b border-border bg-paper">
      <nav className="container flex h-14 items-center gap-6">
        <span className="font-serif text-lg font-semibold text-pine">oneBite</span>
        <div className="flex gap-5">
          {TABS.map((t) => {
            const active = t.href === '/' ? path === '/' : path.startsWith(t.href);
            return (
              <Link key={t.href} href={t.href}
                className={cn('text-sm font-semibold', active ? 'text-pine' : 'text-ink-faint hover:text-ink-soft')}>
                {t.label}
              </Link>
            );
          })}
        </div>
      </nav>
    </header>
  );
}
