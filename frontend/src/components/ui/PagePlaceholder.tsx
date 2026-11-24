import { Card } from '@/components/ui/Card';

export type PagePlaceholderProps = {
  title: string;
  requirements: string[];
  figmaFrame: string;
  owner?: string;
};

export function PagePlaceholder({ title, requirements, figmaFrame, owner }: PagePlaceholderProps) {
  return (
    <Card className="flex flex-col gap-2">
      <h1 className="text-2xl font-bold text-ink">{title}</h1>
      <p className="text-sm text-muted">Requisitos: {requirements.join(', ')}</p>
      <p className="text-sm text-muted">Figma: {figmaFrame}</p>
      <p className="text-sm text-muted">Responsável: {owner ?? '(a definir)'}</p>
    </Card>
  );
}
