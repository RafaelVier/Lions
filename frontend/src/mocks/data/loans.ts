import type { EquipmentResponse, LoanResponse, LoanStatus } from '@/types/api';
import { LOAN_STATUS_LABEL } from '@/types/api';
import { isoDateFromToday, isoDateTimeFromToday } from '@/mocks/data/dates';

const DEFAULT_TERM_DAYS = 30; // RN18

interface LoanSeed {
  beneficiaryId: number;
  beneficiaryName: string;
  loanDaysAgo: number;
  renewals: number;
  returnedDaysAgo?: number; // só nos devolvidos
}

// Os 8 primeiros usam os equipamentos EMPRESTADO (na ordem); os 2 últimos, equipamentos DISPONIVEL.
// Vencimento = loanDate + 30 × (1 + renovações): atrasados há 15, 10 e 5 dias; a vencer em
// 5, 7 e 0 dias (hoje); dois em dia (20 dias).
const ACTIVE_SEEDS: LoanSeed[] = [
  { beneficiaryId: 1, beneficiaryName: 'Maria das Dores Silveira', loanDaysAgo: 45, renewals: 0 },
  { beneficiaryId: 2, beneficiaryName: 'José Antônio Brandão', loanDaysAgo: 70, renewals: 1 },
  { beneficiaryId: 3, beneficiaryName: 'Helena Moraes Duarte', loanDaysAgo: 95, renewals: 2 },
  { beneficiaryId: 4, beneficiaryName: 'Carlos Eduardo Pacheco', loanDaysAgo: 25, renewals: 0 },
  { beneficiaryId: 5, beneficiaryName: 'Lúcia Helena Ferraz', loanDaysAgo: 53, renewals: 1 },
  { beneficiaryId: 6, beneficiaryName: 'Antônio Carlos Medeiros', loanDaysAgo: 30, renewals: 0 },
  { beneficiaryId: 7, beneficiaryName: 'Rosa Maria Tavares', loanDaysAgo: 10, renewals: 0 },
  { beneficiaryId: 8, beneficiaryName: 'Paulo Roberto Camargo', loanDaysAgo: 40, renewals: 1 },
];

const RETURNED_SEEDS: LoanSeed[] = [
  {
    beneficiaryId: 9,
    beneficiaryName: 'Ana Beatriz Lacerda',
    loanDaysAgo: 60,
    renewals: 0,
    returnedDaysAgo: 35,
  },
  {
    beneficiaryId: 10,
    beneficiaryName: 'Sebastião Nogueira Prado',
    loanDaysAgo: 90,
    renewals: 1,
    returnedDaysAgo: 62,
  },
];

// Dias entre hoje e o vencimento (negativo = já venceu).
function expectedOffset(seed: LoanSeed): number {
  return -seed.loanDaysAgo + DEFAULT_TERM_DAYS * (1 + seed.renewals);
}

function buildLoan(
  id: number,
  seed: LoanSeed,
  equipment: EquipmentResponse,
  status: LoanStatus,
  today: Date,
): LoanResponse {
  const offset = expectedOffset(seed);
  const overdue = status === 'ATRASADO';
  const returned = status === 'DEVOLVIDO';
  const signedAt = isoDateTimeFromToday(-seed.loanDaysAgo, '10:15:00', today);
  return {
    id,
    beneficiaryId: seed.beneficiaryId,
    beneficiaryName: seed.beneficiaryName,
    equipmentId: equipment.id,
    patrimonyNumber: equipment.patrimonyNumber,
    equipmentType: equipment.type,
    equipmentTypeDescription: equipment.typeDescription,
    orderId: null,
    status,
    statusDescription: LOAN_STATUS_LABEL[status],
    loanDate: isoDateFromToday(-seed.loanDaysAgo, today),
    initialExpectedReturnDate: isoDateFromToday(-seed.loanDaysAgo + DEFAULT_TERM_DAYS, today),
    expectedReturnDate: isoDateFromToday(offset, today),
    renewalCount: seed.renewals,
    lastRenewalDate:
      seed.renewals > 0
        ? isoDateFromToday(-seed.loanDaysAgo + DEFAULT_TERM_DAYS * seed.renewals, today)
        : null,
    retroactive: false,
    retroactiveJustification: null,
    initialCondition: equipment.condition,
    deliveryNotes: null,
    deliveryPhotoUrl: null,
    termAvailable: true,
    termHashSha256: String(id).padStart(2, '0').repeat(32),
    termGeneratedAt: signedAt,
    signatureType: 'FISICA_DIGITALIZADA',
    signedAt,
    signedByName: seed.beneficiaryName,
    signatureRegisteredBy: 'operador',
    createdBy: 'operador',
    returnedAt: returned
      ? isoDateTimeFromToday(-(seed.returnedDaysAgo ?? 0), '14:30:00', today)
      : null,
    cancelReason: null,
    canceledBy: null,
    canceledAt: null,
    createdAt: signedAt,
    updatedAt: signedAt,
    overdue,
    daysOverdue: overdue ? -offset : 0,
    dueSoon: !overdue && !returned && offset >= 0 && offset <= 7,
  };
}

export function createSeedLoans(
  equipment: EquipmentResponse[],
  today: Date = new Date(),
): LoanResponse[] {
  const loaned = equipment.filter((e) => e.status === 'EMPRESTADO');
  const available = equipment.filter((e) => e.status === 'DISPONIVEL');
  if (loaned.length < ACTIVE_SEEDS.length || available.length < RETURNED_SEEDS.length) {
    throw new Error('Equipamentos de exemplo insuficientes para gerar os empréstimos.');
  }
  const active = ACTIVE_SEEDS.map((seed, i) =>
    buildLoan(i + 1, seed, loaned[i], expectedOffset(seed) < 0 ? 'ATRASADO' : 'ATIVO', today),
  );
  const returned = RETURNED_SEEDS.map((seed, i) =>
    buildLoan(ACTIVE_SEEDS.length + i + 1, seed, available[i], 'DEVOLVIDO', today),
  );
  return [...active, ...returned];
}
