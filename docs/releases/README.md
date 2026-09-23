# Processo de release

O PayFlow usa Git Flow e Semantic Versioning conforme o ADR-0020. Releases são preparados em uma
branch `release/<versão>` criada a partir de `develop`.

## Preparação

1. criar `release/X.Y.Z` a partir de `develop`;
2. retirar `-SNAPSHOT` e alinhar a versão no Maven, nos dois `package.json` e nos lockfiles;
3. atualizar `CHANGELOG.md` e criar `docs/releases/vX.Y.Z.md`;
4. estabilizar a branch, permitindo somente correções e documentação do release;
5. abrir Pull Request para `main` e aguardar todos os checks obrigatórios.

## Publicação

Depois do merge e da CI verde em `main`, executar manualmente o workflow `Release` na branch `main` e
informar a versão sem o prefixo `v`, por exemplo `0.1.0`.

O workflow valida:

- formato Semantic Versioning;
- execução selecionada na `main`;
- igualdade das versões do backend, raiz e frontend;
- existência das notas da versão;
- tag apontando para o commit correto, quando já existir;
- sucesso dos checks obrigatórios no commit de `main`.

Somente então ele cria a tag anotada `vX.Y.Z` e o GitHub Release. Uma execução repetida nunca move uma
tag existente. Se a tag já apontar para outro commit ou o GitHub Release já existir, o workflow falha.

## Sincronização

Depois da publicação:

1. sincronizar o merge de `main` de volta em `develop` por Pull Request;
2. iniciar a próxima versão de desenvolvimento, usando `-SNAPSHOT` no Maven quando aplicável;
3. nunca reutilizar ou movimentar a tag publicada;
4. criar `hotfix/X.Y.Z` a partir de `main` quando uma versão publicada exigir correção urgente.
