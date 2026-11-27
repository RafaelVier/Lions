# PostgreSQL na mesma VM do backend

O workflow manual **Database CI/CD** testa a configuração e as migrations e depois
sobe o serviço `postgres` por SSH. Usa a imagem oficial `postgres:16`; não é
necessário publicar uma imagem própria do banco no Docker Hub.

O PostgreSQL usa o volume externo `lions-postgres-data` e a rede interna
`lions-internal`. A porta `5432` não é publicada na VM. O backend entra nessa rede
e usa `jdbc:postgresql://postgres:5432/lionsdb`.

## Preparar a VM

Use o mesmo usuário configurado em `SSH_USER`, com acesso ao Docker sem `sudo`
interativo. Docker Engine e Docker Compose v2 precisam estar instalados. Confira:

```bash
docker info > /dev/null
docker compose version
sudo install -d -m 750 -o "$(id -un)" -g "$(id -gn)" /opt/lions/database
touch /opt/lions/database/.env.database
chmod 600 /opt/lions/database/.env.database
nano /opt/lions/database/.env.database
```

Preencha o arquivo com os campos de [.env.database.example](.env.database.example):

```dotenv
POSTGRES_PASSWORD='SENHA_FORTE_DO_ADMINISTRADOR'
APP_DB_NAME=lionsdb
APP_DB_USER=lions
APP_DB_PASSWORD='OUTRA_SENHA_FORTE_PARA_APLICACAO'
```

Você pode gerar cada senha com `openssl rand -hex 32`. As senhas ficam apenas na
VM. No arquivo `.env.prod` do backend, use o mesmo `APP_DB_USER` e
`APP_DB_PASSWORD` como `SPRING_DATASOURCE_USERNAME` e
`SPRING_DATASOURCE_PASSWORD`. A senha administrativa não é usada pelo backend.

O usuário `lions` do PostgreSQL é criado sem privilégios de superusuário, criação
de bancos ou criação de roles. Ele é dono do banco da aplicação, para permitir
que o Flyway crie e altere suas tabelas. O usuário Linux `lions` e a role do banco
são identidades independentes, ainda que tenham o mesmo nome.

## Configuração no GitHub e primeiro deploy

O workflow reutiliza `SSH_PRIVATE_KEY` (Secret) e as Variables `SSH_HOST`,
`SSH_USER`, `SSH_PORT` e `SSH_KNOWN_HOSTS`, já configuradas para o backend.

A Variable opcional `DATABASE_DEPLOY_PATH` tem padrão `/opt/lions/database`.
Ela deve ser diferente de `DEPLOY_PATH` (padrão `/opt/lions/backend`), e ambos os
caminhos devem ser absolutos, usando letras, números, `_` e `-` nos segmentos.

1. Envie os workflows e arquivos de infraestrutura para o GitHub.
2. Em **Actions → Database CI/CD → Run workflow**, selecione a branch.
3. Aguarde os testes e o deploy. O serviço inicializa o volume e cria o banco e a
   role da aplicação. O deploy testa uma conexão TCP com essas credenciais.
4. Complete `/opt/lions/backend/.env.prod` com a URL e as credenciais acima, além
   do JWT e da senha inicial do administrador da aplicação.
5. Execute **Backend CI/CD → Run workflow** com a versão desejada. O Flyway aplica
   as migrations contidas na imagem publicada.

O workflow do banco prepara o PostgreSQL; a versão do schema é implantada junto
da versão correspondente do backend. O deploy do banco não inicia o backend.

## CI de migrations

O workflow reutilizável `postgres-tests.yaml` também roda em todos os PRs e
disparos manuais de **Backend CI/CD**, antes de liberar a publicação. Ele usa um
PostgreSQL descartável no runner, com os mesmos scripts de inicialização:

- Confere a inicialização do contexto Spring e a compatibilidade das entidades
  com as migrations, usando `ddl-auto=validate`.
- Exercita os adapters de persistência contra PostgreSQL.
- Aplica V1 em um schema de teste, insere um registro e atualiza para a última
  migration, verificando que o registro permanece e a segunda execução não aplica
  nada novamente.
- Confere que a role da aplicação não é superusuário nem pode criar bancos/roles.
- Gera um backup, substitui o contêiner e verifica persistência dos dados.

Os testes não acessam a VM nem os dados de produção. A porta local `5432` é
publicada somente no runner pelo arquivo `compose.ci.yaml`, que não é enviado à VM.

## Atualizações e backups

Os deploys de backend e banco compartilham um grupo de concorrência para evitar
atualizações simultâneas. Ao executar novamente o workflow do banco, ele baixa a
imagem PostgreSQL 16 e faz backup de um contêiner existente antes de atualizá-lo.
Também há backup antes de cada deploy do backend, antes de iniciar o Flyway.
Se o backup falhar, o deploy é interrompido.

Backups ficam em `/opt/lions/database/backups`, no formato custom do `pg_dump`,
com acesso restrito ao usuário do deploy. Para gerar um manualmente:

```bash
bash /opt/lions/database/backup.sh /opt/lions/database
```

Esses backups locais não protegem contra perda da VM. Copie-os para um destino
externo e acompanhe o espaço em disco; a rotina de deploy não remove backups.

Os arquivos `.env.database` e `.env.prod` não são enviados pelo workflow. A
inicialização de roles/banco e as senhas do ambiente só são aplicadas em um volume
vazio. Alterar o arquivo de ambiente não troca senhas de um banco já criado;
rotações devem ser feitas explicitamente no PostgreSQL e no ambiente da aplicação.

Não apague o volume para corrigir falhas de inicialização. Se a inicialização
ficar incompleta, inspecione os logs e corrija as roles/banco com acesso
administrativo. O deploy não executa `down` em produção nem apaga dados.
Atualizações de versão principal (por exemplo, 16 para 17) exigem um procedimento
de migração próprio e não fazem parte desse workflow.

## Acompanhar no servidor

```bash
cd /opt/lions/database
docker compose --env-file .env.database ps
docker compose --env-file .env.database logs --tail 100 postgres
docker compose --env-file .env.database exec postgres psql -U postgres -d lionsdb
```
