import type { ReactNode } from 'react';
import { AlertCircle, AlertTriangle, CheckCircle2, Info } from 'lucide-react';

export type AlertProps = {
  tone: 'danger' | 'warning' | 'info' | 'success';
  title?: string;
  children: ReactNode;
};

const TONE = {
  danger: { box: 'border-danger/40 bg-danger/10', text: 'text-danger', Icon: AlertCircle },
  warning: { box: 'border-warning/40 bg-warning/10', text: 'text-warning-fg', Icon: AlertTriangle },
  info: { box: 'border-info/40 bg-info/10', text: 'text-info', Icon: Info },
  success: { box: 'border-success/40 bg-success/10', text: 'text-success-fg', Icon: CheckCircle2 },
} as const;

export function Alert({ tone, title, children }: AlertProps) {
  const { box, text, Icon } = TONE[tone];
  const role = tone === 'danger' || tone === 'warning' ? 'alert' : 'status';

  return (
    <div role={role} className={`flex gap-3 rounded-lg border p-3 text-sm ${box}`}>
      <Icon aria-hidden="true" className={`mt-0.5 size-4 shrink-0 ${text}`} />
      <div className="text-ink">
        {title && <p className={`font-semibold ${text}`}>{title}</p>}
        <div>{children}</div>
      </div>
    </div>
  );
}
