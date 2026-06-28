import { AppNav } from '@/components/AppNav';
import { Skeleton } from '@/components/ui/skeleton';

/**
 * 라우트 전환 중(loading.tsx) 보여줄 공용 스켈레톤.
 * AppNav를 함께 렌더해 전환 동안 상단 네비가 사라지지 않도록 한다.
 */
export function ScreenSkeleton() {
  return (
    <>
      <AppNav />
      <main className="container max-w-2xl py-10">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="mt-2 h-8 w-52" />
        <div className="mt-5 flex flex-col gap-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-24 w-full" />
          ))}
        </div>
      </main>
    </>
  );
}
