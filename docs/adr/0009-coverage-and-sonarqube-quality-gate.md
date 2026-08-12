# ADR-0009 — Cobertura e SonarQube como Quality Gate

- Status: Aceito
- Data: 2026-08-12

## Contexto

TypeScript, ESLint, testes e build cobrem riscos diferentes, mas não oferecem uma visão consolidada de cobertura, duplicação, manutenibilidade, confiabilidade e segurança. O projeto também precisa impedir que a análise de qualidade dependa apenas da extensão instalada no editor.

## Decisão

Gerar cobertura do front-end com Vitest/V8 nos formatos texto, HTML e LCOV, exigindo inicialmente 70% de linhas, funções e instruções e 60% de branches. Gerar cobertura do back-end com JaCoCo em HTML e XML, exigindo inicialmente 70% de linhas e 50% de branches.

Antes de um Pull Request, o comando raiz `npm run check` executa formatação, tipos, lint, cobertura e build do front-end e `mvn verify` no back-end. O E2E permanece separado em `npm run check:e2e` por ter custo maior.

Na CI, os jobs de front-end e back-end produzem os relatórios. Um job SonarQube dependente deles baixa cobertura e bytecode Java, analisa o repositório completo e aguarda o Quality Gate. O job falha quando o gate estiver vermelho ou quando a configuração obrigatória estiver ausente.

## Consequências

Cobertura insuficiente falha localmente e na CI, enquanto o SonarQube fornece uma política compartilhada sobre código novo. Percentuais não substituem revisão da qualidade das assertions. O repositório precisa configurar `SONAR_TOKEN` como secret e `SONAR_HOST_URL` como variable; Pull Requests de forks não executam a análise porque não recebem secrets.
