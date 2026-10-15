# Processo de release

O PayFlow usa Git Flow e Semantic Versioning conforme o ADR-0020. Releases são preparados em uma
branch `release/<versão>` criada a partir de `develop`.

## Preparação

1. criar `release/X.Y.Z` a partir de `develop`;
2. retirar `-SNAPSHOT` e alinhar a versão no Maven, nos dois `package.json` e nos lockfiles;
3. atualizar `CHANGELOG.md` e criar `docs/releases/vX.Y.Z.md`;
4. estabilizar a branch, permitindo somente correções e documentação do release;
5. abrir Pull Request para `main` e aguardar todos os checks obrigatórios.

A branch `release/X.Y.Z` deve permanecer no remoto até o workflow `Release` concluir. Ela identifica o
commit candidato que já produziu as imagens aprovadas em homologação.

## Publicação

Depois do merge e da CI verde em `main`, executar manualmente o workflow `Release` na branch `main` e
informar a versão sem o prefixo `v`, por exemplo `0.1.0`.

O workflow valida:

- formato Semantic Versioning;
- execução selecionada na `main`;
- existência de `release/X.Y.Z` e presença de seu commit no histórico selecionado de `main`;
- igualdade das versões do backend, raiz e frontend no commit candidato;
- existência das notas da versão;
- tag apontando para o commit correto, quando já existir;
- sucesso dos checks de construção no candidato e dos checks obrigatórios no commit de `main`.

Somente então ele cria a tag anotada `vX.Y.Z` no commit exato de `release/X.Y.Z` e o GitHub Release. O
merge em `main` pode ter outro SHA, especialmente quando usa merge commit, mas precisa conter o
candidato. Como a tag e a branch de homologação identificam o mesmo commit, produção encontra o mesmo
manifesto e promove os mesmos digests sem rebuild.

Uma execução repetida nunca move uma tag existente. Se a tag já apontar para outro commit ou o GitHub
Release já existir, o workflow falha. A branch de release pode ser removida somente depois da criação
do GitHub Release e da promoção dos artefatos para produção.

## Sincronização

Depois da publicação:

1. sincronizar o merge de `main` de volta em `develop` por Pull Request;
2. iniciar a próxima versão de desenvolvimento, usando `-SNAPSHOT` no Maven quando aplicável;
3. nunca reutilizar ou movimentar a tag publicada;
4. criar `hotfix/X.Y.Z` a partir de `main` quando uma versão publicada exigir correção urgente.
