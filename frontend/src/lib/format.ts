// Formatação para a tela.
// As datas do backend chegam sem fuso (LocalDate "2026-11-16", LocalDateTime "2026-11-16T14:30:00")
// e por isso são lidas como texto: new Date('2026-11-16') seria meia-noite UTC e apareceria como
// 15/11/2026 no Brasil (UTC−3).

const LOCAL_DATE = /^(\d{4})-(\d{2})-(\d{2})$/;
const LOCAL_DATE_TIME = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.\d{1,9})?)?$/;

function isLeapYear(year: number): boolean {
  return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
}

function isValidDate(year: string, month: string, day: string): boolean {
  const m = Number(month);
  const d = Number(day);
  if (m < 1 || m > 12 || d < 1) return false;
  const daysInMonth =
    m === 2 ? (isLeapYear(Number(year)) ? 29 : 28) : [4, 6, 9, 11].includes(m) ? 30 : 31;
  return d <= daysInMonth;
}

// "2026-11-16" → "16/11/2026". Vazio para nulo ou inválido.
export function formatDate(isoDate: string | null | undefined): string {
  const match = isoDate ? LOCAL_DATE.exec(isoDate) : null;
  if (!match) return '';
  const [, year, month, day] = match;
  return isValidDate(year, month, day) ? `${day}/${month}/${year}` : '';
}

// "2026-11-16T14:30:00" → "16/11/2026 14:30". Vazio para nulo, inválido ou com fuso (Z, -03:00).
export function formatDateTime(isoDateTime: string | null | undefined): string {
  const match = isoDateTime ? LOCAL_DATE_TIME.exec(isoDateTime) : null;
  if (!match) return '';
  const [, year, month, day, hour, minute, second = '00'] = match;
  const validTime = Number(hour) <= 23 && Number(minute) <= 59 && Number(second) <= 59;
  return isValidDate(year, month, day) && validTime
    ? `${day}/${month}/${year} ${hour}:${minute}`
    : '';
}

const MASKED_CPF = /^\*{3}\.\d{3}\.\d{3}-\*{2}$/;
const CPF = /^(\d{3})\.?(\d{3})\.?(\d{3})-?(\d{2})$/;

// LGPD (RNF05): esconde os 3 primeiros e os 2 últimos dígitos, como o backend.
// Aceita só dígitos ou o CPF formatado: "123.456.789-09" → "***.456.789-**". Vazio se inválido.
export function maskCpf(cpf: string | null | undefined): string {
  if (!cpf) return '';
  const value = cpf.trim();
  // já veio mascarado do backend
  if (MASKED_CPF.test(value)) return value;
  const match = CPF.exec(value);
  return match ? `***.${match[2]}.${match[3]}-**` : '';
}
