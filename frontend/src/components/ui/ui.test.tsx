import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { Button, PagePlaceholder, PasswordField, StatusBadge, TextField } from '@/components/ui';
import { EQUIPMENT_STATUS_LABEL, LOAN_STATUS_LABEL } from '@/types/api';
import type { EquipmentStatus, LoanStatus } from '@/types/api';

describe('base UI components', () => {
  it('disables the button and announces loading', () => {
    render(<Button loading>Entrar</Button>);
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
    // o spinner interno é decorativo: não entra no nome acessível do botão
    expect(screen.getByRole('button', { name: 'Entrar' })).toBe(button);
  });

  it('links TextField label and error', () => {
    const { rerender } = render(<TextField label="SENHA" />);
    const input = screen.getByLabelText('SENHA');
    expect(input).not.toHaveAttribute('aria-invalid', 'true');
    rerender(<TextField label="SENHA" error="Campo obrigatório" />);
    const invalid = screen.getByLabelText('SENHA');
    expect(invalid).toHaveAttribute('aria-invalid', 'true');
    const errorId = invalid.getAttribute('aria-describedby');
    expect(errorId).toBeTruthy();
    expect(document.getElementById(errorId!)).toHaveTextContent('Campo obrigatório');
  });

  it('renders labelAction outside the label', () => {
    render(<TextField label="SENHA" labelAction={<button type="button">Esqueci</button>} />);
    expect(screen.getByLabelText('SENHA')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Esqueci' }).closest('label')).toBeNull();
  });

  it('toggles password visibility', async () => {
    const user = userEvent.setup();
    render(<PasswordField label="SENHA" />);
    const input = screen.getByLabelText('SENHA');
    expect(input).toHaveAttribute('type', 'password');
    await user.click(screen.getByRole('button', { name: 'Mostrar senha' }));
    expect(input).toHaveAttribute('type', 'text');
    expect(screen.getByRole('button', { name: 'Ocultar senha' })).toBeInTheDocument();
  });

  it.each(Object.keys(EQUIPMENT_STATUS_LABEL))('renders equipment badge %s', (status) => {
    render(<StatusBadge kind="equipment" status={status as EquipmentStatus} />);
    expect(screen.getByText(EQUIPMENT_STATUS_LABEL[status as EquipmentStatus])).toBeInTheDocument();
  });

  it.each(Object.keys(LOAN_STATUS_LABEL))('renders loan badge %s', (status) => {
    render(<StatusBadge kind="loan" status={status as LoanStatus} />);
    expect(screen.getByText(LOAN_STATUS_LABEL[status as LoanStatus])).toBeInTheDocument();
  });

  it('renders the placeholder with requirements, frame and default owner', () => {
    render(
      <PagePlaceholder
        title="Estoque"
        requirements={['RF03', 'RF04']}
        figmaFrame="Estoque / Lista"
      />,
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Estoque' })).toBeInTheDocument();
    expect(screen.getByText('Requisitos: RF03, RF04')).toBeInTheDocument();
    expect(screen.getByText('Figma: Estoque / Lista')).toBeInTheDocument();
    expect(screen.getByText('Responsável: (a definir)')).toBeInTheDocument();
  });
});
