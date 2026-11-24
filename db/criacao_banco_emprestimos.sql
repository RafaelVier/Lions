BEGIN;

CREATE TABLE IF NOT EXISTS tb_endereco (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cep VARCHAR(10),
    cidade VARCHAR(100),
    bairro VARCHAR(100),
    rua VARCHAR(150),
    numero INTEGER,
    complemento VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS tb_beneficiario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_endereco UUID UNIQUE REFERENCES tb_endereco(id),
    cpf VARCHAR(14),
    nome VARCHAR(150) NOT NULL,
    dt_nascimento DATE,
    telefone VARCHAR(20),
    email VARCHAR(150),
    dt_cadastro DATE DEFAULT CURRENT_DATE,
    status CHAR(1),
    observacoes VARCHAR(1000),
    dependente BOOLEAN DEFAULT FALSE,
    nome_resp VARCHAR(150),
    cpf_resp VARCHAR(14),
    telefone_resp VARCHAR(20),
    email_resp VARCHAR(150),
    relacao_resp VARCHAR(100),
    dt_aceite_lgpd TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(150) NOT NULL,
    login VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150),
    senha VARCHAR(255) NOT NULL,
    status CHAR(1),
    cargo CHAR(1)
);

CREATE TABLE IF NOT EXISTS tb_equipamento_tipo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS tb_equipamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_tipo UUID NOT NULL REFERENCES tb_equipamento_tipo(id),
    ano INTEGER,
    status CHAR(1),
    observacoes VARCHAR(1000),
    patrimonio VARCHAR(100),
    conservacao CHAR(1),
    porte CHAR(1)
);

CREATE TABLE IF NOT EXISTS tb_especificacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_equipamento UUID NOT NULL REFERENCES tb_equipamento(id),
    nome VARCHAR(150) NOT NULL,
    valor VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS tb_fila_espera (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dt_solicitacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    prioridade BOOLEAN DEFAULT FALSE,
    justificativa VARCHAR(1000),
    status CHAR(1),
    dt_reserva TIMESTAMP,
    dt_expiracao TIMESTAMP,
    recusas INTEGER DEFAULT 0,
    id_beneficiario UUID NOT NULL REFERENCES tb_beneficiario(id),
    id_tipo UUID NOT NULL REFERENCES tb_equipamento_tipo(id),
    id_equipamento UUID REFERENCES tb_equipamento(id),
    id_usuario UUID REFERENCES tb_usuario(id)
);

CREATE TABLE IF NOT EXISTS tb_emprestimo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_beneficiario UUID NOT NULL REFERENCES tb_beneficiario(id),
    id_equipamento UUID NOT NULL REFERENCES tb_equipamento(id),
    id_usuario UUID NOT NULL REFERENCES tb_usuario(id),
    id_fila_espera UUID REFERENCES tb_fila_espera(id),
    dt_retirada DATE,
    dt_prevista DATE,
    dt_devolucao DATE,
    renovacoes INTEGER DEFAULT 0,
    status CHAR(1),
    estado_devolucao CHAR(1),
    termo_pdf VARCHAR(500),
    motivo_cancelamento VARCHAR(1000),
    observacoes VARCHAR(1000)
);

CREATE TABLE IF NOT EXISTS tb_emprestimo_renovacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_emprestimo UUID NOT NULL REFERENCES tb_emprestimo(id),
    dt_entrega_anterior DATE,
    dt_entrega_nova DATE
);

CREATE TABLE IF NOT EXISTS tb_manutencao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_equipamento UUID NOT NULL REFERENCES tb_equipamento(id),
    id_usuario UUID NOT NULL REFERENCES tb_usuario(id),
    dt_entrada DATE,
    dt_saida DATE,
    descricao VARCHAR(1000),
    custo NUMERIC(12,2),
    parecer VARCHAR(1000),
    status CHAR(1)
);

COMMIT;
