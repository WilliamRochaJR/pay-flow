# ADR-0008 — GitHub Flow para desenvolvimento

- Status: Aceito
- Data: 2026-08-11

## Contexto

O PayFlow é desenvolvido inicialmente por uma pessoa, possui uma linha de produção e entrega mudanças incrementais. O Git Flow clássico acrescentaria branches permanentes de desenvolvimento e release antes de existirem versões simultâneas ou uma equipe que necessite dessa coordenação.

## Decisão

Após os commits da publicação inicial, adotar GitHub Flow. Toda mudança começa em uma branch curta criada a partir de `main`, passa por Pull Request e pelas verificações obrigatórias da CI antes do merge.

Usar prefixos `feature/`, `fix/`, `refactor/`, `test/`, `docs/` e `ci/` conforme a natureza da mudança. A branch `main` representa sempre a versão integrável e potencialmente publicável.

## Consequências

O fluxo permanece simples, oferece revisão e validação automatizada e evita divergência prolongada entre branches permanentes. O GitHub deverá proteger `main` e exigir os jobs `Front-end`, `Back-end` e `E2E`. Git Flow clássico poderá ser reavaliado se surgirem releases simultâneos ou manutenção de múltiplas versões.
