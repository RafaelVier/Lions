import { useState } from 'react';
import { Eye, EyeOff, Lock } from 'lucide-react';
import { TextField } from '@/components/ui/TextField';
import type { TextFieldProps } from '@/components/ui/TextField';

export type PasswordFieldProps = Omit<TextFieldProps, 'type'>;

export function PasswordField({ icon, ...rest }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);
  const Icon = visible ? EyeOff : Eye;

  return (
    <TextField
      {...rest}
      type={visible ? 'text' : 'password'}
      icon={icon ?? <Lock className="size-4" />}
      trailing={
        <button
          type="button"
          aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
          onClick={() => setVisible((v) => !v)}
          className="flex size-8 items-center justify-center rounded text-muted hover:text-ink focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
        >
          <Icon aria-hidden="true" className="size-4" />
        </button>
      }
    />
  );
}
