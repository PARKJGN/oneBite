import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const badgeVariants = cva(
  'inline-flex items-center rounded-pill px-2.5 py-0.5 text-xs font-bold tracking-wide',
  {
    variants: {
      variant: {
        new: 'text-pine',                 // "새 에디션"
        read: 'text-clay',                // "다 읽음" — ceremony 골드
        category: 'text-pine',            // 카테고리 라벨
        solid: 'bg-pine text-white',
      },
    },
    defaultVariants: { variant: 'new' },
  },
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLSpanElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <span className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
