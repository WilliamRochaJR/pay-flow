# ADR-0002 — PostgreSQL como fonte de verdade

- Status: Aceito
- Data: 2026-08-10

## Contexto

Saldo e transferência exigem consistência transacional. Adotar PostgreSQL e MongoDB no início duplicaria operação e criaria dúvidas sobre autoridade dos dados.

## Decisão

Usar PostgreSQL para usuários, contas, saldos e transferências. MongoDB só poderá entrar como projeção de auditoria após a introdução de eventos; nunca será a fonte do saldo.

## Consequências

Débito, crédito e registro da transferência podem ser confirmados atomicamente. Consultas de auditoria avançadas ficam adiadas, e migrations passam a fazer parte obrigatória da entrega.
