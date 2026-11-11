# Spec 001 — Alinhamento do backend à planilha de requisitos

> **Fonte de verdade:** planilha "Requisitos / Regras de Negócio / Casos de Uso / Matriz" linkada no `README.md`.
> Em caso de conflito com `docs/DESCRICAO_DO_PROJETO_E_BACKEND.md`, **prevalece a planilha**.
> **MER:** ainda não disponível (Lucidchart exige login). As entidades seguem a coluna "Entidades principais" da Matriz.
> Campos cuja necessidade é incerta são **opcionais** (nullable) e marcados com `(opc.)` no `plan.md`, para revisão quando o MER for anexado.

## 1. Objetivo

Levar o backend (Spring Boot 3 / Java 21 / DDD) do estado atual — domínio parcial sem camada REST — para uma API funcional que cubra os
"Requisitos decididos" da planilha, respeitando as RN01–RN61.

## 2. Escopo

| RF | Nome | Entra nesta spec? |
|---|---|---|
| RF01 | Gestão de usuários e permissões | ✅ |
| RF02 | Gestão de beneficiários (assistidos) | ✅ |
| RF03 | Gestão de equipamentos | ✅ |
| RF04 | Identificação por QR Code | ✅ (geração + leitura por código) |
| RF05 | Gestão de empréstimos e devoluções | ✅ |
| RF06 | Gestão da fila de espera | ✅ |
| RF07 | Geração de termos em PDF | ✅ |
| RF08 | Registro e histórico de manutenções | ✅ |
| RF09 | Histórico do assistido | ✅ |
| RF10 | Histórico de empréstimos / auditoria | ✅ |
| RF11 | Relatórios (PDF e CSV) | ✅ (Excel via CSV) |
| RF12 | Notificações automáticas | ✅ (canal real = adaptador de log/wa.me; provedores reais SMS/e-mail ficam como porta) |
| RF13 | Assistente inteligente | 🟡 parcial: assistente **baseado em regras** (sugestão de alocação + FAQ). Integração com LLM fica atrás de uma porta, fora do escopo |
| RF14 | Onboarding / ajuda | ✅ backend do conteúdo de ajuda (RN53); UI é do frontend |
| RF15 | Dashboard | ✅ |
| — | Pedido público (fluxo do wireframe / doc interno) | ✅ mantido como "Solicitação" que alimenta empréstimo ou fila |

### Fora do escopo (registrado para próximas specs)
- Funcionamento offline e resolução de conflitos de sincronização (RN60) — depende do frontend PWA.
- Recuperação de senha self-service por e-mail (UC03 parcial: troca de senha autenticada + redefinição pelo Admin entram).
- Provedores reais de SMS/e-mail/WhatsApp Business API.
- Assistente com LLM (RN50–RN52 ficam garantidas pela porta e pelo caráter consultivo).
- Migrations Flyway definitivas — aguardam o MER (dev segue com `ddl-auto: update`).
- Reincorporação de equipamento baixado (comentário do professor na RN13) — conflita com RN12 ("Baixado é estado final"); depende de decisão do cliente.
- Entidades fora da planilha (`LionsClub`, `Member`, `SocialAction`) permanecem intocadas.

## 3. Perfis (RN01)

| Perfil | Role técnica | Pode |
|---|---|---|
| Administrador | `ROLE_ADMIN` | Tudo, incluindo usuários, baixa, parâmetros, cancelamentos, prioridades, auditoria |
| Operador/Voluntário | `ROLE_OPERATOR` | Beneficiários, pedidos, empréstimos, devoluções, renovações, manutenções, fila |
| Consulta | `ROLE_VIEWER` | Somente leitura de dashboard e relatórios, **com dados pessoais mascarados** (RN43/RN56) |

## 4. Requisitos e critérios de aceite

Toda regra listada deve ter teste automatizado.

### RF01 — Usuários e permissões (UC01–UC03)
- **RN01** Toda rota protegida exige a role correspondente; acesso sem permissão → `403`.
- **RN02** Só Admin cria/edita/inativa usuários e altera perfis. Quando um Admin tenta alterar o **próprio** perfil → `422`. Admin não pode inativar a si mesmo.
- **RN03** Usuário nunca é apagado fisicamente; inativação seta `active=false` e bloqueia login.
- **RN57** Senha forte (≥ 8, maiúscula, minúscula, dígito e especial). Token JWT expira após **30 min**; `POST /auth/refresh` renova a sessão (inatividade = não renovar). `POST /auth/logout` revoga o token e registra o encerramento na auditoria. Login também é auditado.
- `/auth/register` público é **removido** (criação só por Admin).

### RF02 — Beneficiários (UC04–UC07)
- **RN04** CPF válido (dígitos verificadores) quando `documentType=CPF`; não pode haver dois beneficiários **ativos** com o mesmo documento. Na duplicidade → `409` com `existingId` no corpo.
- **RN05** Obrigatórios: nome completo, documento, data de nascimento, telefone, endereço (CEP, logradouro, número, bairro, cidade, UF) e contato alternativo (nome + telefone).
- **RN06** Menor de 18 anos ou `legallyIncapable=true` exige responsável legal (nome + documento); o termo é emitido e assinado pelo responsável.
- **RN07** Cadastro só é efetivado com consentimento (`consentGranted=true`) com data/hora gravada pelo sistema e com os canais de notificação autorizados (pode ser vazio).
- **RN08** Inativação recusada (`422`) se houver empréstimo não encerrado, ocorrência aberta ou posição ativa na fila.
- **RN34** Inativação publica evento que remove o beneficiário da fila.
- **RN59** Admin pode anonimizar: dados pessoais substituídos, empréstimos preservados. Não permitido com empréstimo em aberto.

### RF03 — Equipamentos (UC08, UC11, UC12)
- **RN09** Patrimônio gerado pelo sistema (`PT-0001`, `PT-0002`, ...) e imutável (não aceito no create/update).
- **RN11** Status exclusivo: `DISPONIVEL`, `RESERVADO`, `EMPRESTADO`, `EM_MANUTENCAO`, `BAIXADO`.
- **RN12** Transições válidas: DISPONIVEL→{RESERVADO, EMPRESTADO, EM_MANUTENCAO, BAIXADO}; RESERVADO→{EMPRESTADO, DISPONIVEL}; EMPRESTADO→{DISPONIVEL, EM_MANUTENCAO}; EM_MANUTENCAO→{DISPONIVEL, BAIXADO}; BAIXADO é final. Outras → `422`. Mudanças de status ocorrem **somente pelos fluxos** (reserva, empréstimo, devolução, manutenção, baixa); não há endpoint genérico de troca de status (UC11 é atendido pelos fluxos).
- **RN13** Baixa só por Admin, com motivo (`INSERVIVEL`, `PERDA`, `FURTO`, `DOACAO`); baixado não pode ser emprestado nem reservado. Baixa = exclusão lógica (RN03).
- **RN14** Tipo obrigatório (`CADEIRA_DE_RODAS`, `ANDADOR`, `MULETAS`, `CADEIRA_DE_BANHO`, `OUTROS`), porte/tamanho opcional, conservação obrigatória (`BOM`, `REGULAR`, `RUIM`).

### RF04 — QR Code (UC09, UC10)
- **RN10** QR Code codifica **apenas o número de patrimônio**; é determinístico, logo a reimpressão gera o mesmo código.
- **RN15** A leitura (`GET /equipamentos/leitura?codigo=`) só localiza o item e devolve as operações possíveis para o status atual; nunca executa operação.

### RF05 — Empréstimos e devoluções (UC13, UC15–UC17)
- **RN16** Nenhum campo de cobrança/taxa/caução existe no modelo; o termo declara gratuidade.
- **RN17** Só empresta equipamento `DISPONIVEL` ou `RESERVADO` para o próprio beneficiário.
- **RN18** Prazo padrão 30 dias, parametrizável por tipo pelo Admin; data prevista calculada pelo sistema.
- **RN19** Máx. 1 empréstimo ativo do mesmo tipo por beneficiário e máx. 2 ativos no total (parametrizável).
- **RN20** Beneficiário bloqueado se tiver empréstimo em atraso, termo não assinado ou ocorrência aberta.
- **RN21** Empréstimo nasce `PENDENTE_ASSINATURA` com termo gerado; só fica `ATIVO` após registro da assinatura (física digitalizada via upload ou digital/aceite eletrônico). Termo não pode ser excluído.
- **RN22** Data de retirada não pode ser futura; retroativa exige Admin + justificativa.
- **RN23** Renovação: máx. 2 por empréstimo, não pode estar em atraso, não pode haver fila para o tipo, só a partir de 7 dias antes do vencimento. Nova data = vencimento atual + prazo do tipo.
- **RN24** Devolução exige condição. `PRECISA_MANUTENCAO` ou `DANIFICADO` → equipamento vai para `EM_MANUTENCAO` e abre manutenção corretiva automaticamente; `BOM_ESTADO` → `DISPONIVEL`.
- **RN25** Retirada, assinatura, renovação, devolução e cancelamento gravam usuário e data/hora do sistema (não editáveis).
- **RN26** Empréstimo nunca é excluído; só cancelado (Admin + motivo).
- **RN27** Atraso a partir do dia seguinte ao vencimento (rotina diária). Com 30 dias de atraso → ocorrência `INADIMPLENCIA` + aviso ao Admin.
- **RN28** Mesmo equipamento não pode ser emprestado/reservado em duas operações simultâneas (lock pessimista + `@Version`); a segunda → `409`.
- **RN40** Devolução `DANIFICADO` gera ocorrência `DANO` vinculada ao beneficiário e ao empréstimo (bloqueia pela RN20).

### RF06 — Fila de espera (UC18–UC21)
- **RN29** Fila por **tipo** de equipamento, FIFO por data/hora.
- **RN30** Prioridade (`URGENCIA_SAUDE`, `PESSOA_IDOSA`, `POS_OPERATORIO`) só por Admin, com justificativa; prioritários vêm antes, FIFO entre si.
- **RN31** Uma posição ativa por beneficiário por tipo.
- **RN32** Quando um equipamento fica disponível, o sistema reserva para o primeiro da fila (status `RESERVADO`), notifica e dá 48 h.
- **RN33** Reserva expirada ou recusada: item vai ao próximo; beneficiário volta ao fim da fila; após 2 expirações/recusas consecutivas é removido.
- **RN34** Beneficiário inativado ou bloqueado é removido da fila.

### RF07 — Termo em PDF (UC14)
- **RN06/RN21/RN61** PDF com dados do assistido (ou responsável), equipamento, prazos e cláusula de gratuidade; hash SHA-256; arquivo guardado no storage e vinculado ao empréstimo, sem endpoint de exclusão.

### RF08 — Manutenções (UC22–UC25)
- **RN35** Em manutenção não pode ser emprestado/reservado e não conta como disponível.
- **RN36** Abertura exige descrição do defeito, data de entrada e responsável técnico; equipamento passa a `EM_MANUTENCAO` automaticamente.
- **RN37** Encerramento com parecer: `APTO` → `DISPONIVEL`; `INSERVIVEL` → baixa (motivo `INSERVIVEL`) — só Admin.
- **RN38** Custos (peça/serviço) por manutenção, acumulados no histórico do item.
- **RN39** Revisão preventiva sinalizada com 12 meses sem manutenção ou 10 ciclos de empréstimo (parametrizável).
- **RN41** Manutenção encerrada não é editada; correções só por lançamento de ajuste (Admin + motivo).

### RF09 / RF10 — Históricos e auditoria (UC26, UC27)
- **RN41** Registros concluídos imutáveis; correção = novo registro de ajuste com autor e motivo.
- **RN42** Histórico do beneficiário consolida empréstimos, renovações, devoluções, solicitações, fila e ocorrências, inclusive de equipamentos baixados.
- **RN52** Ações automáticas são auditadas com `automatic=true`, data/hora e regra de origem.
- **RN58** Criação, alteração e inativação gravam usuário, data/hora e valor anterior dos campos alterados.

### RF11 — Relatórios (UC28, UC29)
- Tipos: `INVENTARIO`, `EMPRESTIMOS`, `ASSISTIDOS`, `MANUTENCOES`; formatos `PDF` e `CSV`.
- **RN43** Perfil Consulta recebe nome/CPF/endereço/telefone mascarados.
- **RN44** Toda exportação é auditada (quem, quando, filtros).
- **RN45** Relatórios transacionais exigem período; padrão = últimos 30 dias.
- **RN55** Baixados ficam fora de disponibilidade/ocupação, mas aparecem no histórico e nos custos.

### RF12 — Notificações (UC30–UC33)
- **RN46** Avisos 7 dias antes, 1 dia antes, no dia e a cada 3 dias em atraso (sem duplicar no mesmo dia).
- **RN47** Só nos canais autorizados; sem canal → tarefa de contato manual para o Admin.
- **RN48** Falha registrada; até 3 tentativas; depois vira contato manual.
- **RN49** Resumo diário ao Admin: atrasos, reservas expiradas, manutenções abertas, preventivas vencidas.

### RF13 — Assistente (UC34–UC36), baseado em regras
- **RN50** Só sugere (alocação fila × itens disponíveis); nunca executa.
- **RN51** Respostas de FAQ não expõem dados pessoais; sugestões de alocação só para Admin.

### RF14 — Ajuda (UC37, UC38)
- **RN53** Conteúdo (tutorial, dica, FAQ, onboarding) mantido pelo Admin e filtrado pelo perfil; FAQ pública disponível sem login.

### RF15 — Dashboard (UC39, UC40)
- **RN54** Taxa de ocupação = emprestados / ativos (não baixados).
- **RN55** Baixados fora dos indicadores de disponibilidade/ocupação.
- **RN56** Consulta vê apenas números consolidados (sem listas nominais).
- Filtros por período e tipo de equipamento.

## 5. Requisitos não funcionais atendidos pelo backend
- **RNF02** Camadas DDD/hexagonal preservadas; módulos desacoplados por eventos de aplicação.
- **RNF03** Consultas indexadas por status/tipo/beneficiário; sem N+1 nos fluxos críticos.
- **RNF05** Mascaramento, consentimento, anonimização, auditoria e storage de termos.
- **RNF07** Concorrência controlada por lock pessimista + versão otimista no equipamento (RN28).

## 6. Pendências de validação (após o MER)
Todos os campos `(opc.)` do `plan.md`, nomes de tabelas/colunas e a migration Flyway `V1__init_schema.sql`.
