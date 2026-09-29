# ADR-0022 — Métricas internas e alarmes efêmeros

## Status

Aceito.

## Contexto

Health check e logs informam disponibilidade e detalhes de eventos, mas não respondem rapidamente
quantas transferências foram concluídas ou recusadas, quanto tempo elas levam e se a EC2 está falhando
ou sustentando CPU alta. A PoC também não deve manter uma plataforma de observabilidade cara enquanto
fica desligada.

## Decisão

Usar Micrometer, já integrado ao Spring Boot Actuator, para registrar métricas de negócio com nomes e
tags de cardinalidade limitada:

- `payflow.transfers.completed`;
- `payflow.transfers.replayed`;
- `payflow.transfers.rejected`, com `reason` limitado aos códigos conhecidos;
- `payflow.transfers.failed`;
- `payflow.transfers.duration`, com `outcome` limitado.

Contadores de conclusão e replay só são incrementados depois do commit da transação. IDs de usuário,
conta, transferência ou correlação não serão tags, pois criariam séries ilimitadas e aumentariam custo.

Expor `health`, `info` e `metrics` pelo Actuator. O Caddy continuará roteando publicamente somente
`/health` e `/api/*`; portanto, `/actuator/metrics` ficará acessível apenas pela rede/porta interna ou
por acesso administrativo controlado.

Declarar no mesmo Terraform efêmero dois alarmes CloudWatch nativos da EC2:

- falha de status durante dois minutos;
- CPU média acima de 80% durante quinze minutos.

Os alarmes nascem e são destruídos com o ambiente. Nesta etapa não haverá SNS nem e-mail; o estado é
visível no CloudWatch durante a demonstração. Notificação será adicionada somente quando existir um
destino aprovado e necessidade operacional.

## Consequências

O diagnóstico local e administrativo melhora sem expor métricas publicamente ou introduzir Grafana,
Prometheus e CloudWatch Agent agora. Métricas da aplicação ainda não são enviadas ao CloudWatch; essa
integração pode ser adicionada quando o ambiente permanecer ligado por tempo suficiente para justificar
custo e complexidade.

As permissões Terraform passam a incluir somente a gestão de alarmes CloudWatch. O bootstrap de
identidade precisa ser aplicado antes do primeiro deploy que criar esses alarmes.
