# Deploy do backend via SSH

O disparo manual de **Backend CI/CD** testa o backend, publica no Docker Hub e executa
o deploy por SSH. O servidor recebe o `compose.yaml` da revisão selecionada e usa o
digest retornado pela publicação, sem depender da tag `latest`.

Este Compose contém apenas o backend e conecta à rede `lions-internal`. Prepare
primeiro o [PostgreSQL na mesma VM](../database/README.md), pelo workflow
**Database CI/CD**. As migrations Flyway continuam sendo aplicadas pelo backend
no perfil `prod`, após o backup do banco.

## Preparação do servidor (uma vez)

Requisitos: Linux com Bash, Docker Engine e Docker Compose v2 com suporte a
`up --wait --wait-timeout`. A imagem publicada atualmente é Linux AMD64; o servidor
precisa ser compatível. O usuário SSH precisa conseguir executar Docker sem `sudo`
interativo e escrever na pasta de deploy.

1. Crie uma chave SSH dedicada ao deploy, sem senha. Cadastre a chave pública em
   `~/.ssh/authorized_keys` do usuário remoto e guarde a privada no GitHub Secret.
2. Crie a pasta, substituindo `deploy` pelo usuário e grupo corretos:

   ```bash
   sudo install -d -m 750 -o deploy -g deploy /opt/lions/backend
   ```

3. Copie [.env.prod.example](.env.prod.example) para
   `/opt/lions/backend/.env.prod`, preencha os valores e restrinja o acesso:

   ```bash
   chmod 600 /opt/lions/backend/.env.prod
   openssl rand -base64 32 # gere um valor para LIONS_JWT_SECRET
   ```

   Use senhas fortes e mantenha os segredos somente no servidor. As aspas simples
   do exemplo preservam caracteres como `$` nos valores. O host da URL JDBC deve
   ser `postgres` para o banco na mesma VM: `localhost` aponta para o próprio
   backend. Use `jdbc:postgresql://postgres:5432/lionsdb` e a senha
   `APP_DB_PASSWORD` do arquivo `.env.database` do banco.

4. Se a imagem do Docker Hub for privada, execute `docker login` como o mesmo
   usuário do SSH, usando um token com permissão de leitura. O login feito pelo
   workflow para publicar não autentica o servidor.
5. Confira a chave pública do servidor por um canal confiável. O `ssh-keyscan`
   pode coletá-la, mas compare sua impressão digital com a obtida no servidor
   antes de cadastrá-la. Não desative a verificação de host:

   ```bash
   # No computador de administração (ajuste a porta):
   ssh-keyscan -p 22 servidor.exemplo.com > known_hosts
   ssh-keygen -lf known_hosts

   # No servidor, para comparar a chave ED25519:
   sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
   ```

   Para portas diferentes de 22, preserve o formato `[host]:porta` gerado pelo
   `ssh-keyscan`. A porta SSH precisa estar acessível ao runner do GitHub Actions.

## Configuração no GitHub

Em **Settings → Secrets and variables → Actions**, configure:

| Tipo | Nome | Valor |
| --- | --- | --- |
| Secret | `SSH_PRIVATE_KEY` | Chave privada completa, incluindo cabeçalho e rodapé |
| Variable | `SSH_HOST` | DNS ou IPv4 do servidor |
| Variable | `SSH_USER` | Usuário remoto |
| Variable | `SSH_PORT` | Opcional; padrão `22` |
| Variable | `SSH_KNOWN_HOSTS` | Conteúdo verificado de `known_hosts` |
| Variable | `DEPLOY_PATH` | Opcional; padrão `/opt/lions/backend` |
| Variable | `DATABASE_DEPLOY_PATH` | Opcional; padrão `/opt/lions/database` |

O caminho deve ser absoluto, com segmentos compostos de letras, números, `_` ou
`-`. As configurações existentes `DOCKERHUB_IMAGE`, `DOCKERHUB_USERNAME` (Variables)
e `DOCKERHUB_TOKEN` (Secret) continuam sendo usadas na publicação.

## Executar e acompanhar

Em **Actions → Backend CI/CD → Run workflow**, selecione a branch e informe
`new_version`. PRs executam somente os checks, incluindo testes das migrations
contra PostgreSQL descartável. A publicação depende desses testes. O job `deploy`
só começa após a publicação e o registro da versão terminarem com sucesso.
Backend e banco compartilham um grupo de concorrência, inclusive entre branches.

O deploy faz backup pelo script do banco, valida o Compose, baixa a imagem e
recria somente `backend`. O
healthcheck da imagem consulta `/actuator/health`; o job aguarda até 240 segundos
pela saúde do serviço. A execução marca falha se o backend não ficar saudável.
Não há rollback automático, pois a inicialização pode ter aplicado migrations.

Arquivos enviados são atualizados a cada execução; `.env.prod` permanece no
servidor. O volume `lions-backend_storage` persiste os arquivos da aplicação. A
imagem prepara a pasta com permissão de escrita para o usuário `app`.

A porta é publicada por padrão apenas em `127.0.0.1:8080`, para uso com proxy
reverso ou túnel SSH. Para testar do seu computador:

```bash
ssh -L 8080:127.0.0.1:8080 deploy@servidor.exemplo.com
# Em outro terminal:
curl --fail http://localhost:8080/actuator/health
```

Após o primeiro deploy bem-sucedido, `.release.env` registra o digest saudável.
Para consultar o estado e os logs no servidor:

```bash
cd /opt/lions/backend
docker compose --env-file .env.prod --env-file .release.env ps
docker compose --env-file .env.prod --env-file .release.env logs --tail 100 backend
```

Se o primeiro deploy falhar, `.release.env` ainda não existirá. Para diagnosticar,
defina `BACKEND_IMAGE` com o `repositorio@sha256:...` mostrado na execução e use
somente `--env-file .env.prod` nos comandos de consulta. Se uma atualização falhar,
o contêiner pode estar executando a imagem nova com problemas; `.release.env`
continua registrando a última versão saudável.

Para voltar a um digest anterior, primeiro confira a compatibilidade com o schema
atual, copie `deploy.sh` para o servidor e execute:

```bash
bash deploy.sh /opt/lions/backend seuusuario/ortoempresta@sha256:DIGEST_ANTERIOR
```

Esse comando troca a aplicação, sem desfazer migrations. A substituição do único
contêiner causa uma breve indisponibilidade. O deploy não executa `compose down`
nem remove volumes.
