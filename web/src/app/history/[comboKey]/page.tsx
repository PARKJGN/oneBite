'use client';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { useLibraryEditions, type LibraryEdition } from '@/lib/hooks';
import { useEat } from '@/lib/useEat';
import { EditionThumb } from '@/components/EditionThumb';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';

// 에디션 카드 — 탭하면 베어무는 애니메이션 후 상세로(읽었으면 바로)
function EditionCard({ ed }: { ed: LibraryEdition }) {
  const { bite, scale, onClick } = useEat(ed.read, ed.editionId, 0.045);
  return (
    <Card role="button" onClick={onClick} className="cursor-pointer overflow-hidden transition-colors hover:bg-cloud">
      <div className="h-24" style={{ transform: `scale(${scale})` }}>
        <EditionThumb bite={bite} seed={ed.editionId} />
      </div>
      <CardContent className="p-4">
        <p className="line-clamp-2 font-semibold text-ink">{ed.oneLine}</p>
        <div className="mt-2 flex items-center justify-between">
          <span className="text-xs text-ink-faint">{ed.issueDate}</span>
          {bite < 1 && <Badge variant="new">새 에디션</Badge>}
        </div>
      </CardContent>
    </Card>
  );
}

export default function SlotEditionsPage() {
  const params = useParams<{ comboKey: string }>();
  const comboKey = decodeURIComponent(params.comboKey);
  const editions = useLibraryEditions(comboKey);

  return (
    <>
      <main className="container max-w-3xl py-10">
        <Link href="/history" className="text-sm text-pine">← 히스토리</Link>
        <h1 className="mt-3 text-2xl font-bold text-ink">{comboKey.split('+').join(' · ')}</h1>
        <p className="mt-1.5 text-xs text-ink-faint">{editions.data?.length ?? 0}개 에디션</p>

        <div className="mt-6 grid gap-4 sm:grid-cols-2">
          {editions.isLoading && [0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-44 w-full" />)}
          {editions.data?.map((ed) => (
            <EditionCard key={ed.editionId} ed={ed} />
          ))}
        </div>
      </main>
    </>
  );
}
