import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister';
import type { Persister } from '@tanstack/react-query-persist-client';
import { getSession } from '@/features/auth/session';

export const QUERY_CACHE_KEY = 'lions.queryCache';
// O cache guarda dados pessoais (LGPD): vale 24 h e é apagado quando a sessão termina.
export const PERSIST_MAX_AGE_MS = 24 * 60 * 60_000;
// Trocar o valor descarta caches gravados por versões anteriores do app.
export const PERSIST_BUSTER = '1';

export function createQueryPersister(storage: Storage = window.localStorage): Persister {
  // Nunca grava sem sessão. O guard fica no storage (e não no persistClient) porque o persister
  // adia a escrita (throttle): uma gravação pendente poderia sair depois do fim da sessão.
  const guarded: Pick<Storage, 'getItem' | 'setItem' | 'removeItem'> = {
    // Sem sessão não restaura: dados de um usuário anterior não podem chegar a outro.
    getItem: (key) => {
      if (getSession() === null) {
        storage.removeItem(key);
        return null;
      }
      return storage.getItem(key);
    },
    setItem: (key, value) => {
      if (getSession() !== null) storage.setItem(key, value);
    },
    removeItem: (key) => storage.removeItem(key),
  };
  return createSyncStoragePersister({ storage: guarded, key: QUERY_CACHE_KEY });
}
