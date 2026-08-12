# Instruções para agentes

## Objetivo

Construir o PayFlow incrementalmente, preservando um produto executável ao final de cada marco.

## Princípios

- Respeitar os ADRs em `docs/adr/` ou propor um novo ADR antes de contrariá-los.
- Não adicionar Kafka, MongoDB, WebFlux, Kubernetes ou novos serviços antes do marco que os introduz.
- Manter regras de negócio independentes de controllers e detalhes de persistência.
- Tratar valores monetários com `BigDecimal` em Java e `numeric(19,2)` no PostgreSQL; nunca usar ponto flutuante.
- Transferências concluídas são imutáveis. Correções futuras devem usar estorno, não `PUT` ou `DELETE`.
- Toda mudança de comportamento deve incluir testes proporcionais ao risco.
- Nunca versionar segredos. Configuração sensível vem de variáveis de ambiente ou do serviço de segredos adotado.

## Antes de implementar

1. Identificar o marco atual em `docs/plan.md`.
2. Confirmar que a tarefa pertence ao escopo desse marco.
3. Atualizar contrato ou ADR quando a mudança alterar uma decisão pública.

## Arquitetura do front-end

- Organizar funcionalidades por domínio e caso de uso: `features/<dominio>/<caso-de-uso>/`.
- Usar nomes de caso de uso como `create-transfer` e `list-transfers`, evitando pastas técnicas globais.
- Manter o componente principal, modelo, caso de uso e service dentro da pasta do caso de uso.
- Colocar em `components/` somente componentes internos usados pelo componente/caso de uso maior.
- Manter cada teste ao lado do arquivo testado, usando o mesmo nome com `.test.ts` ou `.test.tsx`.
- Usar `*.model.ts` para tipos/dados, `*.service.ts` para comunicação externa e o arquivo sem sufixo para coordenação e regras do caso de uso.
- Não criar arquivos ou pastas vazias para antecipar necessidades. Só separar caso de uso e service quando suas responsabilidades forem reais e distintas.
- Mover código para `shared/` apenas quando ele for reutilizado por mais de uma feature.

## Git e commits

- Nunca executar `git commit` sem antes pedir autorização explícita ao usuário.
- Antes de pedir autorização, apresentar resumidamente quais arquivos e mudanças entrarão no commit e sugerir a mensagem.
- A autorização vale somente para o commit apresentado; alterações ou commits posteriores exigem uma nova confirmação.
- Não adicionar `my-docs/` ao Git, nem mesmo com `git add -f`. Essa pasta é um caderno exclusivamente local.
- Após a publicação inicial, seguir GitHub Flow: criar branch curta a partir de `main`, abrir Pull Request, aguardar a CI e somente então fazer merge.
- Usar prefixos de branch coerentes com a mudança: `feature/`, `fix/`, `refactor/`, `test/`, `docs/` ou `ci/`.
- Não criar branches permanentes `develop` ou `release` enquanto o projeto não tiver ciclos simultâneos de versão que justifiquem Git Flow clássico.

## Critério geral de conclusão

Código formatado, testes relevantes aprovados, documentação coerente e execução reproduzível conforme o README do componente.
