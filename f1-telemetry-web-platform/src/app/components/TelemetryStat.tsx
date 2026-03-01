import { ReactNode } from 'react';
import { cn } from '../components/ui/utils';

interface TelemetryStatProps {
  label: string;
  value: string | number;
  unit?: string;
  variant?: 'neutral' | 'performance' | 'warning' | 'critical';
  size?: 'large' | 'medium' | 'small';
  delta?: number;
  className?: string;
}

export function TelemetryStat({ 
  label, 
  value, 
  unit, 
  variant = 'neutral',
  size = 'medium',
  delta,
  className 
}: TelemetryStatProps) {
  const variants = {
    'neutral': 'text-text-primary',
    'performance': 'text-[#00FF85]',
    'warning': 'text-[#FACC15]',
    'critical': 'text-[#E10600]',
  };

  const sizes = {
    'large': 'text-[2rem] leading-8',
    'medium': 'text-xl leading-6',
    'small': 'text-xs leading-4',
  };

  return (
    <div className={cn('flex flex-col gap-1', className)}>
      <div className="text-xs text-text-secondary uppercase tracking-wider font-medium">
        {label}
      </div>
      <div className="flex items-baseline gap-2">
        <span className={cn('font-bold font-mono', sizes[size], variants[variant])}>
          {value}
        </span>
        {unit && (
          <span className="text-sm text-text-secondary">
            {unit}
          </span>
        )}
        {delta !== undefined && (
          <span className={cn(
            'text-xs font-medium',
            delta > 0 ? 'text-[#00FF85]' : delta < 0 ? 'text-[#E10600]' : 'text-text-secondary'
          )}>
            {delta > 0 ? '+' : ''}{delta.toFixed(1)}
          </span>
        )}
      </div>
    </div>
  );
}
