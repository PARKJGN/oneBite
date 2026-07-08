'use client';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useSlots, useDeleteSlot } from '@/lib/hooks';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';

export default function SlotsPage() {
  const router = useRouter();
  const slots = useSlots();
  const del = useDeleteSlot();
  const count = slots.data?.length ?? 0;

  const onDelete = (id: number) => {
    if (confirm('발송만 중단되고, 받은 에디션은 히스토리에 남아요. 삭제할까요?')) del.mutate(id);
  };

  return (
    <>
      <main className="container max-w-2xl py-10">
        <h1 className="text-2xl font-bold text-ink">내 슬롯</h1>
        <p className="mt-2 text-sm text-ink-soft">변경한 내용은 다음 오전 8시 발송부터 반영돼요.</p>
        <p className="mt-4 text-xs font-semibold text-ink-faint">{count} / 3 슬롯 사용 중</p>

        <div className="mt-3 divide-y divide-border">
          {slots.isLoading && [0, 1, 2].map((i) => <Skeleton key={i} className="my-4 h-7 w-full" />)}
          {slots.data?.map((s) => (
            <div key={s.id} className="flex items-center py-4">
              <span className="flex-1 text-lg font-semibold text-ink">{s.categoryLine}</span>
              <Link href={`/onboard?slotId=${s.id}&codes=${s.categoryCodes.join(',')}`} className="mr-4 text-sm font-semibold text-pine">수정</Link>
              <button onClick={() => onDelete(s.id)} className="text-sm font-semibold text-danger">삭제</button>
            </div>
          ))}
        </div>

        {count < 3 && (
          <Button variant="secondary" className="mt-6 w-full border-dashed" onClick={() => router.push('/onboard')}>
            + 슬롯 추가
          </Button>
        )}
      </main>
    </>
  );
}
