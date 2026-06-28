import * as React from 'react';
import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-md text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        default: 'bg-primary text-primary-foreground hover:bg-forest',     // Pine → Forest
        secondary: 'bg-card text-pine border border-input hover:bg-cloud', // 화이트 + 그린 보더
        ghost: 'text-pine hover:bg-cloud',
        destructive: 'bg-destructive text-destructive-foreground hover:opacity-90',
        ceremony: 'bg-accent text-accent-foreground hover:opacity-90',     // Clay Gold(책갈피/완료)
        link: 'text-pine underline-offset-4 hover:underline',
      },
      size: {
        default: 'h-12 px-4 py-3',
        sm: 'h-9 px-3',
        lg: 'h-14 px-6 text-base',
        pill: 'h-10 px-4 rounded-pill',
        icon: 'h-10 w-10',
      },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : 'button';
    return <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />;
  },
);
Button.displayName = 'Button';

export { Button, buttonVariants };
