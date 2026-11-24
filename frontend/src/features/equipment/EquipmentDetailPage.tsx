import { useParams } from 'react-router';
import { PagePlaceholder } from '@/components/ui';

export function EquipmentDetailPage() {
  const { patrimonio } = useParams();
  return (
    <PagePlaceholder
      title={`Equipamento ${patrimonio}`}
      requirements={['RF03', 'RF05', 'RF07', 'RF08']}
      figmaFrame="Detalhes (11-2155)"
    />
  );
}
