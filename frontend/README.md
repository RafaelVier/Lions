# Lions Conecta - Frontend

Frontend do sistema do banco ortopédico do Lions Clube de Medianeira, feito com React, TypeScript e Tailwind.

Já tem a tela de login, o menu, as rotas protegidas e as páginas das outras telas (ainda vazias). Por enquanto o sistema usa dados de exemplo, então não precisa do backend rodando.

Figma: <https://www.figma.com/design/PSJREjqnsyN5riXIEl8i58/Lions-Club?node-id=0-1>

## Como rodar

Precisa do Node 22 ou mais novo (<https://nodejs.org>).

```bash
cd frontend
npm install
npm run dev
```

Depois abra <http://localhost:5173>.

## Usuários de teste

A senha é `lions2026` para todos.

| Usuário | Perfil |
|---|---|
| `admin` | Administrador (vê tudo, inclusive Usuários) |
| `operador` | Operador |
| `consulta` | Só consulta |

A sessão expira depois de 30 minutos sem uso (RN57).

## Variáveis de ambiente

Para mudar algo só na sua máquina, crie o arquivo `frontend/.env.local` (ele não vai pro git). O `.env.example` mostra todas as opções.

| Variável | Padrão | Para quê |
|---|---|---|
| `VITE_USE_MOCKS` | `true` | `true` usa dados de exemplo, `false` usa a API real |
| `VITE_API_URL` | `/api/v1` | prefixo das chamadas à API |
| `VITE_BACKEND_URL` | `http://localhost:8080` | endereço do backend no `npm run dev` |
| `VITE_MOCK_TOKEN_TTL_SECONDS` | `1800` | validade do token de exemplo (use 120 ou mais) |
| `VITE_MOCK_DELAY_MS` | `400` | atraso das respostas de exemplo, em ms |

Para usar a API real, coloque `VITE_USE_MOCKS=false` no `.env.local` e reinicie o `npm run dev`.

## Pastas

```
frontend/
├── public/          # ícones, logo, favicon
├── branding/        # logo original
└── src/
    ├── app/         # rotas e configuração geral
    ├── api/         # cliente HTTP e configuração da API
    ├── services/    # funções que buscam e enviam dados
    ├── mocks/       # dados de exemplo
    ├── types/       # tipos iguais aos do backend
    ├── features/    # uma pasta por tela (auth, dashboard, equipment, loans...)
    ├── components/  # componentes reutilizáveis (ui) e layout
    ├── hooks/       # hooks compartilhados
    ├── lib/         # utilitários (datas, CPF...)
    └── styles/      # Tailwind, cores e fonte
```

## Regras da equipe

1. As telas não importam nada de `src/mocks/`. Os dados vêm sempre pelos `services/`.
2. Para gravar dados (POST, PUT, DELETE), use `useAppMutation` em vez de `useMutation`. O login é a única exceção.
3. Os dados de exemplo ficam todos em `src/mocks/data/`. Não crie dados soltos dentro da sua tela.
4. Use os componentes de `@/components/ui` e as cores de `src/styles/index.css`.
5. Para mostrar datas e CPF, use `formatDate`, `formatDateTime` e `maskCpf` de `src/lib/format.ts`.

O `npm run lint` e o `npm test` avisam se alguma dessas regras for quebrada.

## Como criar uma tela

A rota, o item do menu e a página vazia de cada tela já existem. Use o login (`src/features/auth/` e `src/services/auth.service.ts`) como exemplo.

1. Confira se o tipo dos dados já existe em `src/types/api.ts`.
2. Crie os dados de exemplo em `src/mocks/data/` e adicione no `src/mocks/store.ts`.
3. Crie o service em `src/services/` (ex.: `waitlist.service.ts`), usando `config.useMocks` para escolher entre os dados de exemplo e a API.
4. Crie um hook na pasta da tela com `useQuery` para ler os dados.
5. Troque o `PagePlaceholder` da página pela tela de verdade.
6. Rode `npm run check` antes de abrir o PR.

## Comandos

| Comando | O que faz |
|---|---|
| `npm run dev` | roda o projeto |
| `npm run build` | gera a versão final em `dist/` |
| `npm test` | roda os testes |
| `npm run lint` | procura problemas no código |
| `npm run format` | formata o código |
| `npm run check` | formata, roda lint, testes e build (rodar antes do PR) |

## Branches

1. Crie sua branch a partir de `frontend`, no formato `feature/frontend/<nome>` (ex.: `feature/frontend/tela-fila-espera`).
2. Abra o PR para a branch `frontend`, não para a `main`.
3. A `frontend` vai para a `main` quando o grupo fechar uma entrega.

## Quem faz qual tela

| Rota | Tela | RF | Responsável |
|---|---|---|---|
| `/login` | Login | RF01 | pronta |
| `/` | Painel | RF15 | |
| `/estoque` | Lista do estoque | RF03 | |
| `/estoque/novo` | Novo equipamento | RF03, RF04 | |
| `/estoque/:patrimonio` | Detalhes do equipamento | RF03, RF05, RF07, RF08 | |
| `/emprestimos` | Empréstimos | RF05 | |
| `/relatorios` | Relatórios | RF11 | |
| `/beneficiarios` | Beneficiários | RF02, RF09 | |
| `/fila` | Fila de espera | RF06 | |
| `/usuarios` | Usuários (só admin) | RF01 | |
| `/ajuda` | Ajuda | RF14 | |

## Deploy (Infra)

O `npm run build` gera a pasta `frontend/dist/`. No NGINX, as rotas do front precisam cair no `index.html`:

```nginx
location / {
    root /caminho/para/frontend/dist;
    try_files $uri $uri/ /index.html;
}
```

O `/api` deve chegar ao backend mantendo o prefixo (`/api/v1/...`). O `.env.production` está com `VITE_USE_MOCKS=true` até o backend ter o login pronto.
