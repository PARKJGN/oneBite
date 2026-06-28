'use client';
import { useParams, useRouter } from 'next/navigation';
import { Bookmark } from 'lucide-react';
import { useEdition, useSetBookmark } from '@/lib/hooks';
import { AppNav } from '@/components/AppNav';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';

export default function EditionDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const edition = useEdition(Number(params.id));
  const setBookmark = useSetBookmark(Number(params.id));
  const e = edition.data;

  return (
    <>
      <AppNav />
      <main className="container max-w-2xl py-10">
        <div className="flex items-center justify-between">
          <button onClick={() => router.back()} className="text-sm text-pine">← 뒤로</button>
          {e && (
            <button onClick={() => setBookmark.mutate(!e.bookmarked)} disabled={setBookmark.isPending}
              className="flex items-center gap-1.5 text-sm font-semibold text-pine disabled:opacity-50">
              <Bookmark className="h-4 w-4" fill={e.bookmarked ? 'currentColor' : 'none'} />
              {e.bookmarked ? '책갈피됨' : '책갈피'}
            </button>
          )}
        </div>
        {!e ? (
          <div className="mt-6 flex flex-col gap-3">
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-8 w-3/4" />
            <Skeleton className="mt-5 h-5 w-28" />
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-24 w-full" />
          </div>
        ) : (
          <article className="mt-4">
            <p className="text-sm text-ink-faint">{e.issueDate}</p>
            <p className="mt-4 text-xs font-bold tracking-widest text-ink-faint">한줄평</p>
            <h1 className="mt-2 font-serif text-2xl font-medium leading-snug text-ink">{e.oneLine}</h1>

            <p className="mt-8 text-xs font-bold tracking-widest text-ink-faint">시장 요약</p>
            {e.marketSummary.map((p, i) => (
              <p key={i} className="mt-3 leading-relaxed text-[#26352c]">{p}</p>
            ))}

            {e.crossInsight && (
              <div className="mt-6 rounded-md bg-cloud p-4">
                <Badge variant="category">카테고리 연결</Badge>
                <p className="mt-2 leading-relaxed text-[#26352c]">{e.crossInsight}</p>
              </div>
            )}

            <p className="mt-8 text-xs font-bold tracking-widest text-ink-faint">참고 뉴스</p>
            <ul className="mt-2 divide-y divide-border">
              {e.items.map((it, i) => (
                <li key={i} className="py-3">
                  <a href={it.url} target="_blank" rel="noreferrer" className="font-medium text-ink hover:text-pine">{it.title}</a>
                  <p className="mt-1 text-xs text-ink-faint">{it.source}</p>
                </li>
              ))}
            </ul>
          </article>
        )}
      </main>
    </>
  );
}
