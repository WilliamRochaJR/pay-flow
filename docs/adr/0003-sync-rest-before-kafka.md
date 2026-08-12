# ADR-0003 — REST síncrono antes de Kafka

- Status: Aceito
- Data: 2026-08-10

## Contexto

O usuário precisa saber imediatamente se a transferência foi aceita. Kafka é útil para efeitos secundários, mas não elimina a necessidade de uma transação consistente no fluxo principal.

## Decisão

Confirmar transferências por REST e PostgreSQL. Quando eventos forem adicionados, gravá-los em uma transactional outbox e publicá-los no Kafka para auditoria e notificações.

## Consequências

O M0/M1 não depende de broker. A evolução evita dual write entre banco e Kafka, ao custo de criar e operar um publicador da outbox no M3.
