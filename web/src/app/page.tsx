'use client';
import { useState } from 'react';
import { useToday, useYesterday, useCategories, type TodaySlot } from '@/lib/hooks';
import { useEat } from '@/lib/useEat';
import { EditionThumb } from '@/components/EditionThumb';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';

// 슬롯별 "오늘의 한 입" 카드 — 탭하면 베어무는 애니메이션 후 상세로(읽었으면 바로)
function TodaySlotCard({ slot }: { slot: TodaySlot }) {
  const { bite, scale, onClick } = useEat(slot.read, slot.editionId);
  return (
    <Card role="button" onClick={onClick} className="cursor-pointer transition-colors hover:bg-cloud">
      <CardContent className="flex items-center gap-4 p-4">
        <div className="h-16 w-16 shrink-0 overflow-hidden rounded-xl" style={{ transform: `scale(${scale})` }}>
          <EditionThumb bite={bite} seed={slot.editionId ?? 0} />
        </div>
        <div className="min-w-0 flex-1">
          <Badge variant="category">{slot.categoryLine}</Badge>
          <p className="mt-1.5 line-clamp-2 text-lg font-semibold leading-snug text-ink">{slot.oneLine ?? '준비 중'}</p>
        </div>
      </CardContent>
    </Card>
  );
}

export default function TodayPage() {
  const today = useToday();
  const [page, setPage] = useState(0);
  const yest = useYesterday(page);
  const categories = useCategories();
  const catName = (code: string) => categories.data?.find((c) => c.code === code)?.name ?? code;

  return (
    <>
      <main className="container max-w-2xl py-10">
        <p className="text-sm text-ink-faint">{today.data?.issueDate ?? ''}</p>
        <h1 className="mt-1 text-3xl font-bold text-ink">오늘의 한 입</h1>

        {today.data?.banner && !today.data.slots.some((s) => s.editionId != null) && (
          <div className="mt-4 rounded-md bg-cloud p-3 text-sm text-forest">{today.data.banner}</div>
        )}

        <div className="mt-5 flex flex-col gap-3">
          {today.isLoading && [0, 1, 2].map((i) => <Skeleton key={i} className="h-24 w-full" />)}
          {today.data?.slots.map((s) => (
            <TodaySlotCard key={s.comboKey} slot={s} />
          ))}
        </div>

        {/* 어제 핵심 뉴스 — 5개씩 페이지네이션 */}
        <section className="mt-10">
          <p className="text-xs font-bold tracking-wide text-pine">어제의 한입, 놓치셨나요?</p>
          <h2 className="mt-1.5 text-xl font-bold text-ink">어제 핵심 뉴스</h2>

          <div className="mt-3 flex flex-col gap-2.5">
            {yest.isLoading && [0, 1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-[68px] w-full" />)}
            {yest.data?.items.map((y, i) => (
              <a key={`${y.editionId}-${i}`} href={y.url} target="_blank" rel="noreferrer">
                <Card className="transition-colors hover:bg-cloud">
                  <CardContent className="flex items-start gap-3 p-4">
                    <span className="w-5 font-semibold text-pine">{page * 5 + i + 1}</span>
                    <div>
                      <p className="text-[10px] font-bold tracking-wide text-pine">{catName(y.categoryCode)}</p>
                      <p className="mt-1 font-semibold text-ink">{y.title}</p>
                      <p className="mt-1 text-xs text-ink-faint">{y.source}</p>
                    </div>
                  </CardContent>
                </Card>
              </a>
            ))}
            {(yest.data?.totalItems ?? 0) === 0 && <p className="text-sm text-ink-faint">어제 받은 뉴스가 없어요.</p>}
          </div>

          {(yest.data?.totalPages ?? 0) > 1 && (
            <div className="mt-4 flex items-center justify-center gap-5">
              <Button variant="ghost" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>‹</Button>
              <span className="text-sm text-ink-soft">{page + 1} / {yest.data!.totalPages}</span>
              <Button variant="ghost" size="sm" disabled={page >= yest.data!.totalPages - 1} onClick={() => setPage((p) => p + 1)}>›</Button>
            </div>
          )}
        </section>
      </main>
    </>
  );
}
