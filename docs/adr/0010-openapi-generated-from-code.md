# ADR-0010 — OpenAPI gerado a partir do código

- Status: Aceito
- Data: 2026-08-12

## Contexto

O M1 precisa de um contrato HTTP consultável e de uma interface simples para explorar a API. Manter manualmente uma especificação separada neste estágio criaria risco de divergência em relação aos controllers e DTOs.

## Decisão

Usar Springdoc OpenAPI com Swagger UI. A especificação será gerada a partir dos endpoints, tipos e anotações do back-end e publicada em `/v3/api-docs`; a interface interativa ficará em `/swagger-ui.html`.

Adicionar somente anotações que tragam informação de negócio que não possa ser inferida claramente do código. Um teste de integração verificará que o contrato e a interface continuam publicados.

## Consequências

A documentação acompanha o código com pouca configuração e pode ser usada durante o desenvolvimento e em demonstrações. A geração code-first não substitui testes de contrato nem garante sozinha que toda descrição seja suficiente. Antes da publicação, o acesso à interface deverá ser reavaliado junto com Spring Security e os ambientes de produção.
