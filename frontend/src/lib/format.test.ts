import { afterEach, describe, expect, it, vi } from 'vitest';
import { formatDate, formatDateTime, maskCpf } from '@/lib/format';

afterEach(() => {
  vi.unstubAllEnvs();
});

describe('formatDate', () => {
  it('formats a LocalDate as DD/MM/AAAA', () => {
    expect(formatDate('2026-11-16')).toBe('16/11/2026');
    expect(formatDate('2024-02-29')).toBe('29/02/2024');
  });

  it('keeps the day in Brazil (UTC−3), where new Date(string) would show the day before', () => {
    vi.stubEnv('TZ', 'America/Sao_Paulo');
    // controle: o jeito ingênuo erra o dia neste fuso
    expect(new Date('2026-11-16').getDate()).toBe(15);

    expect(formatDate('2026-11-16')).toBe('16/11/2026');
    expect(formatDateTime('2026-11-16T00:30:00')).toBe('16/11/2026 00:30');
  });

  it.each([null, undefined, '', 'abc', '16/11/2026', '2026-13-01', '2026-02-29', '2026-04-31'])(
    'returns an empty string for %j',
    (value) => {
      expect(formatDate(value)).toBe('');
    },
  );

  it('rejects a date-time (use formatDateTime)', () => {
    expect(formatDate('2026-11-16T14:30:00')).toBe('');
  });
});

describe('formatDateTime', () => {
  it.each([
    ['2026-11-16T14:30:00', '16/11/2026 14:30'],
    ['2026-11-16T14:30', '16/11/2026 14:30'],
    // LocalDateTime do Java pode trazer fração de segundo
    ['2026-11-16T23:59:59.123456', '16/11/2026 23:59'],
  ])('formats %s as %s', (value, expected) => {
    expect(formatDateTime(value)).toBe(expected);
  });

  it.each([
    null,
    undefined,
    '',
    '2026-11-16',
    '2026-11-16T24:00:00',
    '2026-11-16T14:60',
    '2026-11-31T10:00',
    // com fuso não é LocalDateTime: mostrar a hora sem converter seria errado
    '2026-11-16T14:30:00Z',
    '2026-11-16T14:30:00-03:00',
  ])('returns an empty string for %j', (value) => {
    expect(formatDateTime(value)).toBe('');
  });
});

describe('maskCpf', () => {
  it.each(['12345678909', '123.456.789-09'])('masks %s as ***.456.789-**', (value) => {
    expect(maskCpf(value)).toBe('***.456.789-**');
  });

  it('keeps a CPF that already came masked from the backend', () => {
    expect(maskCpf('***.456.789-**')).toBe('***.456.789-**');
  });

  it.each([null, undefined, '', '1234567890', '123456789012', 'abc.def.ghi-jk'])(
    'returns an empty string for %j',
    (value) => {
      expect(maskCpf(value)).toBe('');
    },
  );
});
