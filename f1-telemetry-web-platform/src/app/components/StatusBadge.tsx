import { ReactNode } from 'react';
import { cn } from '../components/ui/utils';

interface StatusBadgeProps {
  variant: 'active' | 'finished' | 'sc' | 'red-flag' | 'warning' | 'error';
  children: ReactNode;
  className?: string;
}

export function StatusBadge({ variant, children, className }: StatusBadgeProps) {
  const variants = {
    'active': 'bg-[#00E5FF]/10 text-[#00E5FF] shadow-[0_0_0_1px_rgba(0,229,255,0.35),0_0_24px_rgba(0,229,255,0.18)]',
    'finished': 'bg-secondary/50 text-text-secondary border border-border',
    'sc': 'bg-[#FACC15]/10 text-[#FACC15] border border-[#FACC15]/30',
    'red-flag': 'bg-[#E10600]/10 text-[#E10600] shadow-[0_0_0_1px_rgba(225,6,0,0.35),0_0_24px_rgba(225,6,0,0.14)]',
    'warning': 'bg-[#FACC15]/10 text-[#FACC15] border border-[#FACC15]/30',
    'error': 'bg-[#EF4444]/10 text-[#EF4444] border border-[#EF4444]/30',
  };

  return (
    <span className={cn(
      'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium uppercase tracking-wider',
      variants[variant],
      className
    )}>
      {children}
    </span>
  );
}
