import { beforeEach, describe, expect, it } from 'vitest';
import { isoDateFromToday } from '@/mocks/data/dates';
import { mockDb, resetMockDb } from '@/mocks/store';
import type { EquipmentStatus, EquipmentType } from '@/types/api';

const today = new Date(2026, 10, 16);
const todayIso = '2026-11-16';

function dayNumber(iso: string): number {
  const [y, m, d] = iso.split('-').map(Number);
  return Date.UTC(y, m - 1, d) / 86_400_000;
}

const TYPES: EquipmentType[] = [
  'CADEIRA_DE_RODAS',
  'CADEIRA_DE_BANHO',
  'ANDADOR',
  'MULETAS',
  'OUTROS',
];
const STATUSES: EquipmentStatus[] = [
  'DISPONIVEL',
  'RESERVADO',
  'EMPRESTADO',
  'EM_MANUTENCAO',
  'BAIXADO',
];
const PREFIX: Record<EquipmentType, string> = {
  CADEIRA_DE_RODAS: 'CR',
  CADEIRA_DE_BANHO: 'CB',
  ANDADOR: 'AN',
  MULETAS: 'MU',
  OUTROS: 'OU',
};

describe('mock dataset', () => {
  beforeEach(() => resetMockDb(today));

  it('formats local dates relative to today', () => {
    expect(isoDateFromToday(0, today)).toBe(todayIso);
    expect(isoDateFromToday(-16, today)).toBe('2026-10-31');
    expect(isoDateFromToday(45, today)).toBe('2026-12-31');
  });

  it('has 24 equipment covering every type (≥2) and every status (≥1)', () => {
    expect(mockDb.equipment).toHaveLength(24);
    for (const type of TYPES) {
      expect(mockDb.equipment.filter((e) => e.type === type).length).toBeGreaterThanOrEqual(2);
    }
    for (const status of STATUSES) {
      expect(mockDb.equipment.filter((e) => e.status === status).length).toBeGreaterThanOrEqual(1);
    }
  });

  it('uses unique patrimony numbers matching the type prefix', () => {
    const numbers = mockDb.equipment.map((e) => e.patrimonyNumber);
    expect(new Set(numbers).size).toBe(numbers.length);
    for (const e of mockDb.equipment) {
      expect(e.patrimonyNumber).toMatch(/^LCM-(CR|CB|AN|MU|OU)-\d{4}$/);
      expect(e.patrimonyNumber.split('-')[1]).toBe(PREFIX[e.type]);
    }
  });

  it('has 10 loans pointing to existing equipment', () => {
    expect(mockDb.loans).toHaveLength(10);
    for (const loan of mockDb.loans) {
      const equipment = mockDb.equipment.find((e) => e.id === loan.equipmentId);
      expect(equipment).toBeDefined();
      expect(loan.patrimonyNumber).toBe(equipment?.patrimonyNumber);
      expect(loan.equipmentType).toBe(equipment?.type);
      expect(loan.equipmentTypeDescription).toBe(equipment?.typeDescription);
    }
  });

  it('links every EMPRESTADO item to exactly one ATIVO/ATRASADO loan and vice versa', () => {
    const open = mockDb.loans.filter((l) => l.status === 'ATIVO' || l.status === 'ATRASADO');
    const loaned = mockDb.equipment.filter((e) => e.status === 'EMPRESTADO');
    expect(open).toHaveLength(loaned.length);
    for (const e of loaned) {
      expect(open.filter((l) => l.equipmentId === e.id)).toHaveLength(1);
    }
    for (const l of open) {
      expect(mockDb.equipment.find((e) => e.id === l.equipmentId)?.status).toBe('EMPRESTADO');
    }
    for (const l of mockDb.loans.filter((x) => x.status === 'DEVOLVIDO')) {
      expect(l.returnedAt).not.toBeNull();
      expect(mockDb.equipment.find((e) => e.id === l.equipmentId)?.status).not.toBe('EMPRESTADO');
    }
  });

  it('uses the 30-day default term and at most 2 renewals', () => {
    for (const l of mockDb.loans) {
      expect(dayNumber(l.initialExpectedReturnDate ?? '') - dayNumber(l.loanDate)).toBe(30);
      expect(l.renewalCount).toBeLessThanOrEqual(2);
      expect(dayNumber(l.expectedReturnDate) - dayNumber(l.initialExpectedReturnDate ?? '')).toBe(
        30 * l.renewalCount,
      );
    }
  });

  it('marks overdue loans from the day after the expected date', () => {
    for (const l of mockDb.loans.filter((x) => x.status !== 'DEVOLVIDO')) {
      const diff = dayNumber(todayIso) - dayNumber(l.expectedReturnDate);
      expect(l.status === 'ATRASADO').toBe(l.expectedReturnDate < todayIso);
      expect(l.overdue).toBe(l.status === 'ATRASADO');
      expect(l.daysOverdue).toBe(l.overdue ? diff : 0);
    }
  });

  it('includes at least 2 overdue, 2 due-this-week and 2 returned loans', () => {
    expect(mockDb.loans.filter((l) => l.overdue).length).toBeGreaterThanOrEqual(2);
    expect(mockDb.loans.filter((l) => l.dueSoon).length).toBeGreaterThanOrEqual(2);
    expect(mockDb.loans.filter((l) => l.status === 'DEVOLVIDO').length).toBeGreaterThanOrEqual(2);
    for (const l of mockDb.loans.filter((x) => x.dueSoon)) {
      const diff = dayNumber(l.expectedReturnDate) - dayNumber(todayIso);
      expect(diff).toBeGreaterThanOrEqual(0);
      expect(diff).toBeLessThanOrEqual(7);
      expect(l.overdue).toBe(false);
    }
  });

  it('keeps the same mockDb object after a reset', () => {
    const ref = mockDb;
    mockDb.loans.pop();
    resetMockDb(today);
    expect(mockDb).toBe(ref);
    expect(mockDb.loans).toHaveLength(10);
  });
});
