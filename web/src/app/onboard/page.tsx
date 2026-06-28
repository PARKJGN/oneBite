'use client';
import { Suspense, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useCategories, useCreateSlot, useUpdateSlot } from '@/lib/hooks';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

const MAX = 4; // 슬롯당 카테고리 최대 4개(FR-003)

function OnboardInner() {
  const router = useRouter();
  const sp = useSearchParams();
  const slotIdParam = sp.get('slotId');
  const editSlotId = slotIdParam ? Number(slotIdParam) : null;
  const isEdit = editSlotId != null;
  const initial = (sp.get('codes') ?? '').split(',').filter(Boolean);

  const categories = useCategories();
  const create = useCreateSlot();
  const update = useUpdateSlot();
  const [selected, setSelected] = useState<string[]>(initial);

  const toggle = (code: string) =>
    setSelected((prev) =>
      prev.includes(code) ? prev.filter((c) => c !== code) : prev.length < MAX ? [...prev, code] : prev,
    );

  const pending = create.isPending || update.isPending;

  const onSave = () => {
    if (selected.length === 0) { alert('카테고리를 하나 이상 선택해 주세요.'); return; }
    if (isEdit) {
      update.mutate({ slotId: editSlotId!, categoryCodes: selected }, {
        onSuccess: () => router.push('/slots'),
        onError: () => alert('수정에 실패했어요. 잠시 후 다시 시도해 주세요.'),
      });
      return;
    }
    create.mutate(selected, {
      onSuccess: () => router.push('/'),
      onError: () => alert('저장에 실패했어요. 잠시 후 다시 시도해 주세요.'),
    });
  };

  return (
    <main className="mx-auto max-w-xl px-8 py-16">
      <p className="text-xs font-bold tracking-wide text-pine">슬롯 만들기</p>
      <h1 className="mt-2.5 text-2xl font-semibold leading-snug text-ink">관심 카테고리를 묶어<br />슬롯을 만드세요</h1>
      <p className="mt-3 text-ink-soft">슬롯은 최대 3개, 슬롯당 카테고리는 최대 4개까지 고를 수 있어요.</p>

      <div className="mt-7 flex items-baseline justify-between">
        <span className="text-xs font-bold text-[#7a8b80]">카테고리</span>
        <span className="text-xs font-semibold text-pine">{selected.length} / {MAX} 선택됨</span>
      </div>

      <div className="mt-3 flex flex-wrap gap-2.5">
        {categories.data?.map((c) => {
          const on = selected.includes(c.code);
          return (
            <button key={c.code} onClick={() => toggle(c.code)}
              className={cn('rounded-pill border px-4 py-2 text-sm font-medium transition-colors',
                on ? 'border-pine bg-pine text-white' : 'border-input bg-card text-[#3a4f43] hover:bg-cloud')}>
              {c.name}
            </button>
          );
        })}
      </div>

      <Button className="mt-7 w-full" onClick={onSave} disabled={pending}>
        {pending ? '저장 중…' : isEdit ? '변경 저장' : '이 슬롯 저장하고 시작'}
      </Button>

      <div className="mt-5 rounded-md bg-cloud p-4">
        <p className="text-sm font-semibold text-pine">알림을 켜면 매일 오전 8시에 묶음 요약을 보내드려요</p>
        <p className="mt-1.5 text-xs leading-relaxed text-ink-soft">지금 거부해도 앱은 계속 쓸 수 있고, 나중에 설정에서 바꿀 수 있어요.</p>
      </div>
    </main>
  );
}

// useSearchParams는 Suspense 경계 필요(App Router CSR bailout)
export default function OnboardPage() {
  return (
    <Suspense fallback={null}>
      <OnboardInner />
    </Suspense>
  );
}
