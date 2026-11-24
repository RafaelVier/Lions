import { config } from '@/api/config';

// Simula a latência da rede; com 0 ms resolve já na próxima microtarefa.
export function mockDelay(ms: number = config.mockDelayMs): Promise<void> {
  return new Promise((resolve) => {
    if (ms <= 0) resolve();
    else setTimeout(resolve, ms);
  });
}
