# Plano de entrega

## Estratégia

Cada marco termina com software demonstrável. A arquitetura cresce por necessidade observável, não para preencher uma lista de tecnologias.

## M0 — fatia vertical local ✅

Objetivo: provar o fluxo React → API → banco antes de investir em cloud.

Escopo:

- monorepo com `frontend/`, `backend/` e `compose.yaml`;
- React, TypeScript e Vite;
- Java 21, Spring Boot, Spring Web, Validation e Data JPA;
- PostgreSQL e migrations com Flyway;
- contas de demonstração carregadas por seed;
- criação e consulta de transferências;
- transferência atômica com validação de saldo;
- testes unitários da regra de transferência e teste de integração com PostgreSQL.
- qualidade do front com TypeScript, ESLint, Prettier e Vitest;
- hooks locais com Husky, lint-staged e Commitlint;
- GitHub Actions validando front-end e back-end.
- Playwright validando o fluxo E2E de transferência em Chromium.

Fora do escopo: login, Kafka, MongoDB, WebFlux, microserviços e AWS.

Critério de aceite: a partir de um clone limpo, um comando sobe o banco, a API e o front; uma transferência criada na UI altera os saldos e aparece no histórico.

Status: implementado em 2026-08-10.

## M1 — primeiro release público

Objetivo: disponibilizar uma demonstração segura e reproduzível.

Escopo:

- cadastro/login com Spring Security e JWT de curta duração;
- contas pertencentes ao usuário autenticado;
- tratamento padronizado de erros e documentação OpenAPI;
- proteção contra transferência duplicada com `Idempotency-Key`;
- Dockerfiles e Docker Compose de produção;
- deploy em uma EC2 com Caddy como proxy HTTPS;
- fundação AWS reproduzível com Terraform, sem aplicar automaticamente a partir de Pull Requests;
- PostgreSQL no mesmo host inicialmente, com volume persistente e backup automatizado;
- evolução do GitHub Actions para realizar deploy após as verificações;
- health checks, logs estruturados e limites básicos de requisição;
- aviso explícito de ambiente fictício e dados de demonstração.

Critério de aceite: uma pessoa abre a URL pública, cria usuário, recebe saldo fictício, faz uma transferência e consulta o histórico. CI impede merge com testes quebrados e o deploy é repetível.

## M2 — confiabilidade e qualidade

- refresh token ou sessão renovável, se necessário;
- paginação e filtros no histórico;
- concorrência segura com lock/versionamento e testes concorrentes;
- Testcontainers nas integrações;
- métricas com Spring Boot Actuator e alarmes básicos;
- análise estática e cobertura no CI;
- evolução da infraestrutura como código para recursos gerenciados e múltiplos ambientes.

Critério de promoção: falhas ou manutenção do M1 mostram que essas capacidades têm valor concreto.

## M3 — eventos sem microserviços

- tabela `outbox_events` gravada na mesma transação da transferência;
- Kafka em ambiente local e serviço compatível/gerenciado em cloud;
- publicação de `TransferCompleted` com contrato versionado;
- consumidor de auditoria ainda dentro do monólito, idempotente;
- retries, dead-letter topic e rastreabilidade por correlation ID.

Critério de promoção: o fluxo síncrono está estável e existe necessidade demonstrável de executar efeitos secundários sem aumentar a latência da transferência.

## M4 — extração do Audit Service

- extrair apenas o consumidor de auditoria;
- MongoDB como projeção de histórico de eventos, não como fonte do saldo;
- deploy e pipeline independentes;
- observabilidade distribuída e teste de contrato.

Critério de promoção: o consumidor possui ciclo de vida, escala ou disponibilidade diferentes da API principal.

## M5 — capacidades avançadas opcionais

- Notification Service consumindo eventos;
- WebFlux para streaming de eventos/SSE ou integração externa altamente concorrente;
- RDS, múltiplas zonas e serviços AWS gerenciados conforme custo e objetivo;
- front-end em CDN e API atrás de load balancer;
- análise de fraude, limites por usuário e estornos.

Esses itens não são requisitos para declarar o portfólio pronto.

## Backlog inicial em ordem

1. Criar esqueleto do monorepo e Compose local.
2. Modelar `User`, `Account` e `Transfer` no banco.
3. Implementar caso de uso transacional de transferência.
4. Expor os endpoints do M0 e padronizar erros.
5. Criar dashboard, formulário e histórico.
6. Adicionar testes e documentação de execução.
7. Iniciar o M1 por autenticação e isolamento dos dados.
8. Containerizar, criar CI e publicar na AWS.

## Definição de pronto do portfólio

- URL pública funcional e HTTPS;
- repositório com README, diagrama, ADRs e instruções de execução;
- dados exclusivamente fictícios;
- pipeline verde;
- demonstração gravada ou screenshots;
- decisões e trade-offs explicáveis em entrevista;
- custo mensal e procedimento de desligamento documentados.
