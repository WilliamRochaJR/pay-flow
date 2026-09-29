# Observabilidade

O PayFlow combina health checks, logs estruturados, correlation ID, métricas internas e alarmes
efêmeros. Cada sinal responde a uma pergunta diferente:

| Sinal       | Pergunta principal                                       |
| ----------- | -------------------------------------------------------- |
| Health      | A aplicação está pronta para receber tráfego?            |
| Logs        | O que ocorreu em uma requisição específica?              |
| Métricas    | Quantas operações ocorreram e quanto tempo elas levaram? |
| Alarmes AWS | A instância está falhando ou sustentando CPU alta?       |

## Métricas de transferência

Com o back-end local em execução, liste as métricas:

```bash
curl http://localhost:8080/actuator/metrics
```

Consulte uma métrica específica:

```bash
curl http://localhost:8080/actuator/metrics/payflow.transfers.completed
curl 'http://localhost:8080/actuator/metrics/payflow.transfers.rejected?tag=reason:insufficient-balance'
curl http://localhost:8080/actuator/metrics/payflow.transfers.duration
```

As métricas existem somente na memória do processo. Reiniciar a API zera os contadores. Isso é
adequado ao ambiente efêmero inicial; retenção histórica exigirá um coletor externo no futuro.

IDs não são usados como tags. `reason` e `outcome` possuem conjuntos pequenos e controlados para
evitar alta cardinalidade.

## Acesso no ambiente publicado

O proxy público não encaminha `/actuator/metrics`. Depois de entrar na instância por Systems Manager,
um operador pode consultar o endpoint de dentro do container:

```bash
sudo docker compose \
  --env-file .env.production \
  -f compose.prod.yaml \
  exec api \
  curl --silent http://localhost:8080/actuator/metrics
```

O endpoint `/health` continua público porque é necessário ao deploy. Métricas operacionais não devem
ser expostas diretamente à internet.

## Alarmes CloudWatch

O Terraform declara dois alarmes ligados à EC2 do ambiente:

- `payflow-<ambiente>-instance-status-check`;
- `payflow-<ambiente>-high-cpu`.

Eles não enviam e-mail nesta etapa. Durante uma publicação, seus estados podem ser consultados em
CloudWatch → Alarms. Como pertencem ao Terraform efêmero, são removidos no `destroy` junto com a EC2.

Antes do primeiro deploy com alarmes, o bootstrap de identidade precisa atualizar a role de
infraestrutura. Essa aplicação altera permissões AWS e exige autorização explícita.

## Evolução futura

Quando houver necessidade de histórico e dashboards, o caminho planejado é adicionar o registry
Prometheus do Micrometer, coletar as métricas por uma solução interna e usar Grafana como camada de
visualização. Essa evolução não exige alterar os nomes atuais das métricas de negócio.
