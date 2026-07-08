'use client';
import Link from 'next/link';
import { useLibrarySlots } from '@/lib/hooks';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

export default function HistoryPage() {
  const slots = useLibrarySlots();

  return (
    <>
      <main className="container max-w-2xl py-10">
        <h1 className="text-3xl font-bold text-ink">히스토리</h1>
        <p className="mt-1.5 text-sm text-ink-soft">슬롯별로 받은 에디션을 모았어요.</p>

        <div className="mt-6 flex flex-col gap-3">
          {slots.isLoading && [0, 1, 2].map((i) => <Skeleton key={i} className="h-20 w-full" />)}
          {slots.data?.map((sl) => (
            <Link key={sl.comboKey} href={`/history/${encodeURIComponent(sl.comboKey)}`}>
              <Card className="transition-colors hover:bg-cloud">
                <CardContent className="flex items-center justify-between p-4">
                  <div>
                    <p className="font-serif text-lg font-semibold text-ink">
                      {sl.categoryLine}{!sl.active && <span className="ml-2 text-xs text-ink-faint">· 보관됨</span>}
                    </p>
                    <p className="mt-2 text-xs text-ink-faint">
                      {sl.editionCount}개 에디션 받음{sl.latestDate ? ` · 최근 ${sl.latestDate}` : ''}
                    </p>
                  </div>
                  <span className="text-xl text-ink-faint">›</span>
                </CardContent>
              </Card>
            </Link>
          ))}
          {slots.data?.length === 0 && <p className="text-sm text-ink-faint">아직 받은 에디션이 없어요.</p>}
        </div>
      </main>
    </>
  );
}
