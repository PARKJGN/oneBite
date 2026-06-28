import { cn } from '@/lib/utils';

// 로딩 자리표시자. tailwind animate-pulse + 톤은 bg-cloud.
export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('animate-pulse rounded-md bg-cloud', className)} />;
}
