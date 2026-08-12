# ADR-0005 — Transferências imutáveis e idempotentes

- Status: Aceito
- Data: 2026-08-10

## Contexto

Um CRUD genérico permite editar ou apagar fatos financeiros e repetir um débito após timeout/retry. Isso simplifica endpoints, mas enfraquece o caso de negócio que o portfólio pretende demonstrar.

## Decisão

Não expor `PUT` ou `DELETE` para transferências. No release público, exigir chave de idempotência na criação. Uma correção será representada futuramente por uma nova operação de estorno vinculada à original.

## Consequências

A API deixa de ser um CRUD literal, mas representa melhor um domínio financeiro. Será necessário armazenar a chave e retornar de forma consistente o resultado de uma repetição.
