# ADR-0001 — Monólito modular primeiro

- Status: Aceito
- Data: 2026-08-10

## Contexto

O projeto precisa demonstrar um fluxo full-stack publicável. Vários serviços desde o início aumentariam deploys, contratos e observabilidade antes de existir carga ou equipes independentes.

## Decisão

Construir uma única aplicação Spring Boot organizada por módulos de negócio. Um serviço só poderá ser extraído quando precisar de ciclo de vida, escala ou disponibilidade próprios.

## Consequências

O primeiro release é mais simples de desenvolver, testar e operar. Os limites de módulos precisam ser respeitados para evitar um monólito desorganizado. A futura extração continua sendo trabalho explícito, não uma promessa automática.
