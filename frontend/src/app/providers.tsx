import { useEffect, useRef, useState, type ReactNode } from 'react';
import type { QueryClient } from '@tanstack/react-query';
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { createQueryPersister, PERSIST_BUSTER, PERSIST_MAX_AGE_MS } from '@/app/persist';
import { createAppQueryClient } from '@/app/queryClient';
import { AuthProvider } from '@/features/auth/AuthContext';
import { getSession, subscribe } from '@/features/auth/session';

export function AppProviders({
  children,
  queryClient,
}: {
  children: ReactNode;
  queryClient?: QueryClient;
}) {
  const [defaultClient] = useState(createAppQueryClient);
  const [persister] = useState(() => createQueryPersister());
  const client = queryClient ?? defaultClient;
  // dono do cache em memória: troca de usuário sem fim de sessão explícito também o descarta
  const cacheOwner = useRef<string | null>(getSession()?.username ?? null);

  useEffect(() => {
    // cache de uma sessão que acabou com o app fechado não pode ser reaproveitado
    if (getSession() === null) void persister.removeClient();
    // qualquer fim de sessão (sair, expirar, inatividade, 401) apaga os dados pessoais em cache
    return subscribe((session, reason) => {
      if (session === null && reason !== null) {
        client.clear();
        void persister.removeClient();
      }
      if (session !== null) {
        if (cacheOwner.current !== null && cacheOwner.current !== session.username) {
          client.clear();
          void persister.removeClient();
        }
        cacheOwner.current = session.username;
      } else {
        cacheOwner.current = null;
      }
    });
  }, [client, persister]);

  return (
    <PersistQueryClientProvider
      client={client}
      persistOptions={{ persister, maxAge: PERSIST_MAX_AGE_MS, buster: PERSIST_BUSTER }}
    >
      <AuthProvider>{children}</AuthProvider>
    </PersistQueryClientProvider>
  );
}
