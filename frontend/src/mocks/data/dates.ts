function pad(n: number): string {
  return String(n).padStart(2, '0');
}

// Data local no formato YYYY-MM-DD, deslocada em `days` dias a partir de `today`.
export function isoDateFromToday(days: number, today: Date = new Date()): string {
  const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + days);
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

// LocalDateTime sem fuso (YYYY-MM-DDTHH:mm:ss).
export function isoDateTimeFromToday(days: number, time: string, today: Date = new Date()): string {
  return `${isoDateFromToday(days, today)}T${time}`;
}
