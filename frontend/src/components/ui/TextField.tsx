import { useId } from 'react';
import type { InputHTMLAttributes, ReactNode, Ref } from 'react';
import { cn } from '@/lib/cn';

export type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
  icon?: ReactNode;
  labelAction?: ReactNode;
  ref?: Ref<HTMLInputElement>;
};

type InternalProps = TextFieldProps & {
  /** Conteúdo à direita dentro do campo (usado pelo PasswordField). */
  trailing?: ReactNode;
};

export function TextField({
  label,
  error,
  icon,
  labelAction,
  trailing,
  className,
  id,
  ref,
  ...rest
}: InternalProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const errorId = `${inputId}-error`;

  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center justify-between gap-2">
        <label
          htmlFor={inputId}
          className="text-[11px] font-semibold uppercase tracking-wide text-muted"
        >
          {label}
        </label>
        {labelAction}
      </div>
      <div className="relative flex items-center">
        {icon && (
          <span aria-hidden="true" className="pointer-events-none absolute left-3 flex text-muted">
            {icon}
          </span>
        )}
        <input
          ref={ref}
          id={inputId}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className={cn(
            'h-11 w-full rounded-lg border bg-field text-sm text-ink placeholder:text-muted-fg',
            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary',
            error ? 'border-danger' : 'border-transparent',
            icon ? 'pl-10' : 'pl-3',
            trailing ? 'pr-11' : 'pr-3',
            className,
          )}
          {...rest}
        />
        {trailing && <span className="absolute right-2 flex">{trailing}</span>}
      </div>
      {error && (
        <p id={errorId} className="text-xs text-danger">
          {error}
        </p>
      )}
    </div>
  );
}
