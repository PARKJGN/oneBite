'use client';
import Link from 'next/link';
import { useBookmarks } from '@/lib/hooks';
import { AppNav } from '@/components/AppNav';
import { Card, CardContent } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';

export default function BookmarksPage() {
  const bookmarks = useBookmarks();

  return (
    <>
      <AppNav />
      <main className="container max-w-2xl py-10">
        <h1 className="text-3xl font-bold text-ink">책갈피</h1>
        <p className="mt-1.5 text-sm text-ink-soft">책갈피한 에디션은 영구 보관돼요.</p>

        <div className="mt-6 flex flex-col gap-3">
          {bookmarks.isLoading && [0, 1, 2].map((i) => <Skeleton key={i} className="h-20 w-full" />)}
          {bookmarks.data?.map((b) => (
            <Link key={b.editionId} href={`/editions/${b.editionId}`}>
              <Card className="transition-colors hover:bg-cloud">
                <CardContent className="p-4">
                  <p className="text-[10px] font-bold tracking-wide text-pine">{b.comboKey.split('+').join(' · ')}</p>
                  <p className="mt-1 line-clamp-2 font-semibold text-ink">{b.oneLine}</p>
                  <p className="mt-1.5 text-xs text-ink-faint">{b.issueDate}</p>
                </CardContent>
              </Card>
            </Link>
          ))}
          {bookmarks.data?.length === 0 && <p className="text-sm text-ink-faint">아직 책갈피한 에디션이 없어요.</p>}
        </div>
      </main>
    </>
  );
}
