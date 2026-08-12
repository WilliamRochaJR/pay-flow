# ADR-0007 — Verificações locais rápidas e CI completa

- Status: Aceito
- Data: 2026-08-11

## Contexto

O projeto precisa manter tipos, estilo, testes e builds confiáveis desde o primeiro marco. Executar toda a suíte de integração a cada commit tornaria o feedback local lento, enquanto depender somente de hooks locais permitiria que verificações fossem ignoradas ou variassem entre máquinas.

## Decisão

Usar TypeScript, ESLint, Prettier e Vitest como verificações do front-end. Playwright cobre um fluxo E2E crítico em Chromium contra a aplicação completa executada pelo Docker Compose. Husky conecta os hooks do Git: o `pre-commit` chama lint-staged para executar ESLint e Prettier somente nos arquivos preparados, e o `commit-msg` chama Commitlint com Conventional Commits.

GitHub Actions é a fonte autoritativa de integração contínua. Em pushes e pull requests para `main`, executa todas as verificações do front-end, `mvn verify` no back-end e o E2E em job independente. As dependências são instaladas de forma reproduzível a partir dos lockfiles e os caches dos gerenciadores reduzem o tempo sem substituir a instalação.

## Consequências

Commits recebem feedback rápido e pull requests validam o projeto completo em ambiente limpo. Hooks podem ser ignorados localmente e dependem da instalação npm, portanto não são uma barreira de segurança; a proteção efetiva exige configurar a branch `main` para exigir os jobs da CI. Formatação automática pode alterar arquivos staged, então o desenvolvedor deve revisar o diff antes de autorizar o commit.
