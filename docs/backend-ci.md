# Backend CI e publicação

Todo pull request executa `clean build bootJar`, incluindo os testes disponíveis,
e salva os relatórios por sete dias. PRs não acessam o Docker Hub nem alteram a
versão no repositório.

Para publicar, abra **Actions > Backend CI > Run workflow**, selecione uma branch
e informe `new_version`, por exemplo `1.0.0` ou `1.1.0-SNAPSHOT`.
O resumo da execução mostra a versão atual e a versão solicitada.

A execução manual atualiza a versão no checkout, compila e testa novamente,
transfere o JAR testado ao job de publicação e publica a imagem no Docker Hub.
Após a publicação, grava a versão em `backend/build.gradle.kts` com um commit na
branch selecionada. Se a versão já for igual, não cria commit.

As tags da imagem são a versão informada e `sha-<commit de origem>`. Na branch
padrão também é publicada a tag `latest`.

## Configuração

Em **Settings > Secrets and variables > Actions**, configure:

| Tipo | Nome | Valor |
| --- | --- | --- |
| Variable | `DOCKERHUB_USERNAME` | Usuário do Docker Hub |
| Variable | `DOCKERHUB_IMAGE` | Repositório completo, como `minha-org/lions-backend` |
| Secret | `DOCKERHUB_TOKEN` | Token com permissão de escrita no Docker Hub |

O job de publicação solicita `contents: write` para salvar a versão usando
`GITHUB_TOKEN`. As políticas do repositório e da branch precisam permitir esse
push; o workflow não contorna proteção de branches nem usa force push. Se outro
commit chegar à branch durante o build, o push da versão será recusado.
Como o commit ocorre após o envio ao Docker Hub, uma falha no push pode deixar
a imagem publicada e a versão ainda não persistida no Git.

## Script local

Com Node.js instalado, na raiz do repositório:

```sh
# Mostrar a versão atual
node backend/scripts/version.mjs

# Alterar a versão em backend/build.gradle.kts
node backend/scripts/version.mjs 1.0.0
```

O script valida o formato da versão antes de alterar o arquivo. Localmente,
o commit fica por conta do desenvolvedor. O pipeline faz o commit somente na
publicação manual bem-sucedida.
