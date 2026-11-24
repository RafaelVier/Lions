import type { EquipmentResponse, LoanResponse } from '@/types/api';
import { createSeedEquipment } from '@/mocks/data/equipment';
import { createSeedLoans } from '@/mocks/data/loans';

export interface MockDb {
  equipment: EquipmentResponse[];
  loans: LoanResponse[];
}

function seed(today?: Date): MockDb {
  const equipment = createSeedEquipment(today);
  return { equipment, loans: createSeedLoans(equipment, today) };
}

// Único repositório em memória; as telas o alteram só por meio dos services.
export const mockDb: MockDb = seed();

// Recria os dados de exemplo mantendo a identidade de `mockDb`.
export function resetMockDb(today?: Date): void {
  const fresh = seed(today);
  mockDb.equipment = fresh.equipment;
  mockDb.loans = fresh.loans;
}
