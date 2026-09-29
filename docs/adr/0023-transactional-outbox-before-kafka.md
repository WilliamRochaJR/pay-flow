# ADR-0023 — Outbox transacional antes do Kafka

## Status

Proposto.

## Contexto

O fluxo financeiro síncrono está estável, mas efeitos secundários futuros, como auditoria e
notificação, não devem aumentar a latência nem comprometer a transferência. Publicar diretamente no
Kafka depois do commit cria uma janela em que a transferência existe no PostgreSQL, mas o evento pode
ser perdido. Publicar antes do commit cria o problema inverso: consumidores podem observar uma
transferência que depois seja revertida.

O projeto continua sendo uma PoC de baixo custo. Introduzir Kafka, serviço de auditoria, MongoDB e
infraestrutura gerenciada ao mesmo tempo tornaria difícil identificar qual decisão resolveu cada
necessidade.

## Decisão proposta

Introduzir eventos em quatro incrementos demonstráveis, mantendo a API como monólito modular:

1. gravar uma linha em `outbox_events` na mesma transação PostgreSQL que conclui a transferência;
2. adicionar Kafka local como dependência opcional e publicar eventos pendentes por um relay interno;
3. consumir o evento em um módulo de auditoria dentro da mesma aplicação, com deduplicação
   persistente;
4. adicionar retries limitados, dead-letter topic e métricas antes de avaliar Kafka em cloud.

```mermaid
flowchart TD
    Request[POST /api/v1/transfers] --> Transaction[Transação PostgreSQL]
    Transaction --> Transfer[(transfers)]
    Transaction --> Outbox[(outbox_events)]
    Outbox --> Relay[Outbox relay]
    Relay --> Kafka[Kafka local]
    Kafka --> Audit[Consumidor de auditoria no monólito]
    Audit --> Processed[(processed_events)]
```

Kafka não participa da confirmação da transferência. PostgreSQL continua sendo a fonte de verdade do
saldo e do estado financeiro.

## Contrato inicial

Publicar `TransferCompleted.v1` como JSON, contendo somente dados necessários:

- `eventId`, UUID global usado para deduplicação;
- `eventType` e `eventVersion` explícitos;
- `occurredAt` em UTC;
- `correlationId` para rastreabilidade;
- `transferId` como identificador do agregado e chave da mensagem;
- `sourceAccountId`, `destinationAccountId`, `amount` como decimal textual e `currency`.

O evento não transportará nome, e-mail, senha, token ou outros dados pessoais. Mudanças incompatíveis
criam uma nova versão de evento; consumidores não dependem diretamente das entidades JPA.

## Semântica de entrega

A entrega será **pelo menos uma vez**. Depois de publicar, o relay marca o registro como publicado. Se
o processo cair entre essas operações, o evento pode ser reenviado. Por isso, o consumidor registra
`(consumer_name, event_id)` com restrição única antes de executar seu efeito observável.

O relay deve processar lotes pequenos e permitir concorrência segura com lock de linha e
`SKIP LOCKED`. Eventos que excederem o limite de tentativas deixam de bloquear o lote e seguem para a
estratégia de falha definida no quarto incremento.

## Limites desta decisão

- não extrair um microserviço no M3;
- não adicionar MongoDB, WebFlux ou Kubernetes;
- não colocar Kafka no caminho síncrono da transferência;
- não publicar infraestrutura Kafka na AWS sem um ADR específico de custo, segurança e operação;
- não prometer entrega exatamente uma vez entre PostgreSQL e Kafka.

## Consequências

A transferência e a intenção de publicar o evento passam a ser atômicas, enquanto a entrega aos
consumidores permanece assíncrona e tolerante a repetição. Em contrapartida, o sistema ganha tabelas
operacionais, um relay e rotinas de retenção e monitoramento.

Começar pela outbox permite testar a garantia principal antes de subir o broker. Kafka local será
opt-in para não tornar o ciclo comum de desenvolvimento mais pesado. A escolha de um serviço Kafka em
cloud fica adiada até existir necessidade demonstrável e uma opção compatível com o orçamento da PoC.

## Critérios para aceitar este ADR

- contrato `TransferCompleted.v1` revisado;
- retenção e tratamento de falhas da outbox definidos;
- testes de atomicidade, reenvio e deduplicação planejados;
- custo operacional local e em cloud explicitado;
- confirmação de que auditoria continuará dentro do monólito durante o M3.
