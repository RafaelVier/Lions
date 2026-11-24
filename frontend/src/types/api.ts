// Espelha os DTOs do backend em origin/wip/onda0-fundacao (16/11/2026). Ajustar quando o contrato mudar.

export type UserRole = 'ROLE_ADMIN' | 'ROLE_OPERATOR' | 'ROLE_VIEWER';
export type EquipmentStatus =
  'DISPONIVEL' | 'RESERVADO' | 'EMPRESTADO' | 'EM_MANUTENCAO' | 'BAIXADO';
export type EquipmentType =
  'CADEIRA_DE_RODAS' | 'CADEIRA_DE_BANHO' | 'ANDADOR' | 'MULETAS' | 'OUTROS';
export type EquipmentCondition = 'BOM' | 'REGULAR' | 'RUIM';
export type LoanStatus = 'PENDENTE_ASSINATURA' | 'ATIVO' | 'ATRASADO' | 'DEVOLVIDO' | 'CANCELADO';
export type OrderStatus =
  'PENDENTE_ANALISE' | 'APROVADO' | 'REJEITADO' | 'EM_FILA_ESPERA' | 'CANCELADO';
export type DocumentType = 'CPF' | 'ESTRANGEIRO' | 'CNPJ';
export type SignatureType = 'FISICA_DIGITALIZADA' | 'DIGITAL';
export type WriteOffReason = 'INSERVIVEL' | 'PERDA' | 'FURTO' | 'DOACAO';

export const USER_ROLE_LABEL: Record<UserRole, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_OPERATOR: 'Operador',
  ROLE_VIEWER: 'Consulta',
};

export const EQUIPMENT_STATUS_LABEL: Record<EquipmentStatus, string> = {
  DISPONIVEL: 'Disponível',
  RESERVADO: 'Reservado',
  EMPRESTADO: 'Emprestado',
  EM_MANUTENCAO: 'Em manutenção',
  BAIXADO: 'Baixado',
};

export const EQUIPMENT_TYPE_LABEL: Record<EquipmentType, string> = {
  CADEIRA_DE_RODAS: 'Cadeira de Rodas',
  CADEIRA_DE_BANHO: 'Cadeira de Banho',
  ANDADOR: 'Andador',
  MULETAS: 'Muletas',
  OUTROS: 'Outros',
};

export const EQUIPMENT_CONDITION_LABEL: Record<EquipmentCondition, string> = {
  BOM: 'Bom',
  REGULAR: 'Regular',
  RUIM: 'Ruim',
};

export const LOAN_STATUS_LABEL: Record<LoanStatus, string> = {
  PENDENTE_ASSINATURA: 'Pendente de assinatura',
  ATIVO: 'Ativo',
  ATRASADO: 'Atrasado',
  DEVOLVIDO: 'Devolvido',
  CANCELADO: 'Cancelado',
};

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  type: 'Bearer';
  username: string;
  email: string;
  role: UserRole;
}

export interface ProblemDetail {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
}

// Datas (LocalDate/LocalDateTime) chegam como string ISO.
export interface EquipmentResponse {
  id: number;
  patrimonyNumber: string;
  type: EquipmentType;
  typeDescription: string;
  size: string | null;
  condition: EquipmentCondition | null;
  conditionDescription: string | null;
  status: EquipmentStatus;
  statusDescription: string;
  notes: string | null;
  photoUrl: string | null;
  reservedForBeneficiaryId: number | null;
  writeOffReason: WriteOffReason | null;
  writeOffNotes: string | null;
  writeOffDate: string | null;
  writeOffBy: string | null;
  lastMaintenanceDate: string | null;
  loanCyclesSinceMaintenance: number;
  version: number | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface LoanResponse {
  id: number;
  beneficiaryId: number | null;
  beneficiaryName: string | null;
  equipmentId: number | null;
  patrimonyNumber: string | null;
  equipmentType: EquipmentType | null;
  equipmentTypeDescription: string | null;
  orderId: number | null;
  status: LoanStatus;
  statusDescription: string;
  loanDate: string;
  initialExpectedReturnDate: string | null;
  expectedReturnDate: string;
  renewalCount: number;
  lastRenewalDate: string | null;
  retroactive: boolean;
  retroactiveJustification: string | null;
  initialCondition: EquipmentCondition | null;
  deliveryNotes: string | null;
  deliveryPhotoUrl: string | null;
  termAvailable: boolean;
  termHashSha256: string | null;
  termGeneratedAt: string | null;
  signatureType: SignatureType | null;
  signedAt: string | null;
  signedByName: string | null;
  signatureRegisteredBy: string | null;
  createdBy: string | null;
  returnedAt: string | null;
  cancelReason: string | null;
  canceledBy: string | null;
  canceledAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  overdue: boolean;
  daysOverdue: number;
  dueSoon: boolean;
}

export interface LoanOrderResponse {
  id: number | null;
  beneficiaryId: number | null;
  beneficiaryName: string | null;
  documentType: DocumentType | null;
  maskedDocument: string | null;
  phone: string | null;
  city: string | null;
  state: string | null;
  forSelf: boolean;
  requesterName: string | null;
  requesterRelationship: string | null;
  equipmentType: EquipmentType | null;
  equipmentTypeDescription: string | null;
  needReason: string | null;
  status: OrderStatus | null;
  statusDescription: string | null;
  rejectionReason: string | null;
  assistedRegistration: boolean;
  decidedBy: string | null;
  decidedAt: string | null;
  createdAt: string | null;
}

export interface DashboardIndicatorsResponse {
  totalEquipments: number;
  availableEquipments: number;
  loanedEquipments: number;
  maintenanceEquipments: number;
  pendingOrdersCount: number;
  pendingOrders: LoanOrderResponse[] | null;
  dueThisWeekLoans: LoanResponse[] | null;
  overdueLoans: LoanResponse[] | null;
}
