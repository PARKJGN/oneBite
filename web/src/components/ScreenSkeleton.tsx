import { Skeleton } from '@/components/ui/skeleton';

/**
 * 라우트 전환 중(loading.tsx) 페이지 영역에 보여줄 공용 스켈레톤.
 * 헤더·하단 탭바는 루트 레이아웃의 AppChrome 이 항상 유지하므로 여기선 본문만 그린다.
 */
export function ScreenSkeleton() {
  return (
    <main className="container max-w-2xl py-10">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="mt-2 h-8 w-52" />
        <div className="mt-5 flex flex-col gap-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
    </main>
  );
}
