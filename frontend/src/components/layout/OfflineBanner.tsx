import { OFFLINE_MESSAGE } from '@/hooks/useAppMutation';
import { useOnlineStatus } from '@/hooks/useOnlineStatus';

export function OfflineBanner() {
  const online = useOnlineStatus();
  if (online) return null;
  return (
    <div
      role="status"
      className="border-b border-warning/40 bg-warning/10 px-4 py-2 text-center text-xs text-warning-fg"
    >
      {OFFLINE_MESSAGE}
    </div>
  );
}
