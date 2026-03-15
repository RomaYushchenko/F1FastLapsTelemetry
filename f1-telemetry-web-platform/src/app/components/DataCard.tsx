import { ReactNode } from 'react';
import { cn } from '../components/ui/utils';

interface DataCardProps {
  title?: string;
  children: ReactNode;
  variant?: 'default' | 'live' | 'warning' | 'error';
  actions?: ReactNode;
  className?: string;
  noPadding?: boolean;
}

export function DataCard({ 
  title, 
  children, 
  variant = 'default',
  actions,
  className,
  noPadding = false
}: DataCardProps) {
  const variants = {
    'default': 'bg-card border border-border',
    'live': 'bg-card border border-[#00E5FF]/30 shadow-[0_0_0_1px_rgba(0,229,255,0.35),0_0_24px_rgba(0,229,255,0.18)]',
    'warning': 'bg-card border-2 border-[#FACC15]/50',
    'error': 'bg-card border-2 border-[#EF4444]/50',
  };

  return (
    <div className={cn('rounded-xl overflow-hidden', variants[variant], className)}>
      {title && (
        <div className="relative z-10 px-4 py-3 border-b border-border/50 flex items-center justify-between bg-inherit">
          <h3 className="text-lg font-semibold">{title}</h3>
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </div>
      )}
      <div className={noPadding ? '' : 'p-4'}>
        {children}
      </div>
    </div>
  );
}
