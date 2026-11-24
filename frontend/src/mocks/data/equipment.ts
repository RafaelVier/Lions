import type {
  EquipmentCondition,
  EquipmentResponse,
  EquipmentStatus,
  EquipmentType,
} from '@/types/api';
import {
  EQUIPMENT_CONDITION_LABEL,
  EQUIPMENT_STATUS_LABEL,
  EQUIPMENT_TYPE_LABEL,
} from '@/types/api';
import { isoDateFromToday, isoDateTimeFromToday } from '@/mocks/data/dates';

const TYPE_CODE: Record<EquipmentType, string> = {
  CADEIRA_DE_RODAS: 'CR',
  CADEIRA_DE_BANHO: 'CB',
  ANDADOR: 'AN',
  MULETAS: 'MU',
  OUTROS: 'OU',
};

interface EquipmentSeed {
  type: EquipmentType;
  size: string | null;
  condition: EquipmentCondition;
  status: EquipmentStatus;
  notes: string | null;
}

// 24 itens: 8 emprestados (um por empréstimo ativo/atrasado), o resto nos demais status.
const SEEDS: EquipmentSeed[] = [
  { type: 'CADEIRA_DE_RODAS', size: 'M', condition: 'BOM', status: 'EMPRESTADO', notes: null },
  { type: 'CADEIRA_DE_RODAS', size: 'G', condition: 'BOM', status: 'EMPRESTADO', notes: null },
  {
    type: 'CADEIRA_DE_RODAS',
    size: 'M',
    condition: 'REGULAR',
    status: 'EMPRESTADO',
    notes: 'Pneu traseiro com desgaste.',
  },
  { type: 'CADEIRA_DE_RODAS', size: 'P', condition: 'BOM', status: 'DISPONIVEL', notes: null },
  { type: 'CADEIRA_DE_RODAS', size: 'M', condition: 'BOM', status: 'DISPONIVEL', notes: null },
  {
    type: 'CADEIRA_DE_RODAS',
    size: 'G',
    condition: 'REGULAR',
    status: 'EM_MANUTENCAO',
    notes: 'Troca de rodízio dianteiro.',
  },
  { type: 'CADEIRA_DE_RODAS', size: 'M', condition: 'BOM', status: 'RESERVADO', notes: null },
  { type: 'CADEIRA_DE_RODAS', size: 'M', condition: 'RUIM', status: 'BAIXADO', notes: null },
  { type: 'CADEIRA_DE_BANHO', size: 'M', condition: 'BOM', status: 'EMPRESTADO', notes: null },
  { type: 'CADEIRA_DE_BANHO', size: 'G', condition: 'BOM', status: 'EMPRESTADO', notes: null },
  { type: 'CADEIRA_DE_BANHO', size: 'M', condition: 'BOM', status: 'DISPONIVEL', notes: null },
  { type: 'CADEIRA_DE_BANHO', size: 'M', condition: 'REGULAR', status: 'DISPONIVEL', notes: null },
  {
    type: 'CADEIRA_DE_BANHO',
    size: 'P',
    condition: 'RUIM',
    status: 'EM_MANUTENCAO',
    notes: 'Reparo na trava das rodas.',
  },
  { type: 'ANDADOR', size: null, condition: 'BOM', status: 'EMPRESTADO', notes: null },
  { type: 'ANDADOR', size: null, condition: 'BOM', status: 'EMPRESTADO', notes: null },
  { type: 'ANDADOR', size: null, condition: 'REGULAR', status: 'DISPONIVEL', notes: null },
  { type: 'ANDADOR', size: null, condition: 'BOM', status: 'DISPONIVEL', notes: null },
  { type: 'ANDADOR', size: null, condition: 'BOM', status: 'DISPONIVEL', notes: null },
  { type: 'MULETAS', size: 'M', condition: 'BOM', status: 'EMPRESTADO', notes: 'Par completo.' },
  { type: 'MULETAS', size: 'G', condition: 'BOM', status: 'DISPONIVEL', notes: 'Par completo.' },
  { type: 'MULETAS', size: 'P', condition: 'RUIM', status: 'BAIXADO', notes: null },
  {
    type: 'OUTROS',
    size: null,
    condition: 'BOM',
    status: 'DISPONIVEL',
    notes: 'Cama hospitalar articulada.',
  },
  {
    type: 'OUTROS',
    size: null,
    condition: 'BOM',
    status: 'DISPONIVEL',
    notes: 'Colchão pneumático.',
  },
  {
    type: 'OUTROS',
    size: null,
    condition: 'BOM',
    status: 'RESERVADO',
    notes: 'Elevador de transferência.',
  },
];

export function createSeedEquipment(today: Date = new Date()): EquipmentResponse[] {
  const counters: Partial<Record<EquipmentType, number>> = {};
  return SEEDS.map((seed, index) => {
    const sequence = (counters[seed.type] ?? 0) + 1;
    counters[seed.type] = sequence;
    const createdAt = isoDateTimeFromToday(-400 + index * 5, '09:00:00', today);
    const writtenOff = seed.status === 'BAIXADO';
    return {
      id: index + 1,
      patrimonyNumber: `LCM-${TYPE_CODE[seed.type]}-${String(sequence).padStart(4, '0')}`,
      type: seed.type,
      typeDescription: EQUIPMENT_TYPE_LABEL[seed.type],
      size: seed.size,
      condition: seed.condition,
      conditionDescription: EQUIPMENT_CONDITION_LABEL[seed.condition],
      status: seed.status,
      statusDescription: EQUIPMENT_STATUS_LABEL[seed.status],
      notes: seed.notes,
      photoUrl: null,
      reservedForBeneficiaryId: seed.status === 'RESERVADO' ? 100 + index : null,
      writeOffReason: writtenOff ? 'INSERVIVEL' : null,
      writeOffNotes: writtenOff ? 'Estrutura comprometida, sem conserto viável.' : null,
      writeOffDate: writtenOff ? isoDateFromToday(-20, today) : null,
      writeOffBy: writtenOff ? 'admin' : null,
      lastMaintenanceDate: seed.status === 'EM_MANUTENCAO' ? isoDateFromToday(-3, today) : null,
      loanCyclesSinceMaintenance: seed.status === 'EMPRESTADO' ? 2 : 0,
      version: 0,
      createdAt,
      updatedAt: createdAt,
    };
  });
}
