import { EQUIPMENT_STATUS_LABEL, LOAN_STATUS_LABEL } from '@/types/api';
import type { EquipmentStatus, LoanStatus } from '@/types/api';

export type StatusBadgeProps =
  { kind: 'equipment'; status: EquipmentStatus } | { kind: 'loan'; status: LoanStatus };

type Tone = 'success' | 'info' | 'warning' | 'danger' | 'muted';

const TONE_CLASS: Record<Tone, string> = {
  success: 'bg-success/15 text-success-fg',
  info: 'bg-info/15 text-info',
  warning: 'bg-warning/15 text-warning-fg',
  danger: 'bg-danger/15 text-danger',
  muted: 'bg-muted/15 text-muted-fg',
};

const EQUIPMENT_TONE: Record<EquipmentStatus, Tone> = {
  DISPONIVEL: 'success',
  RESERVADO: 'info',
  EMPRESTADO: 'warning',
  EM_MANUTENCAO: 'danger',
  BAIXADO: 'muted',
};

const LOAN_TONE: Record<LoanStatus, Tone> = {
  PENDENTE_ASSINATURA: 'warning',
  ATIVO: 'info',
  ATRASADO: 'danger',
  DEVOLVIDO: 'success',
  CANCELADO: 'muted',
};

export function StatusBadge(props: StatusBadgeProps) {
  const tone = props.kind === 'equipment' ? EQUIPMENT_TONE[props.status] : LOAN_TONE[props.status];
  const label =
    props.kind === 'equipment'
      ? EQUIPMENT_STATUS_LABEL[props.status]
      : LOAN_STATUS_LABEL[props.status];

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold ${TONE_CLASS[tone]}`}
    >
      <span aria-hidden="true" className="size-1.5 rounded-full bg-current" />
      {label}
    </span>
  );
}
