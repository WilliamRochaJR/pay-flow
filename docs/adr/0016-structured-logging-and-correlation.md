# ADR 0016 — Logs estruturados e correlação HTTP

## Status

Aceito.

## Decisão

Cada requisição recebe um `X-Correlation-ID` no formato UUID. A API preserva um UUID válido enviado
pelo cliente ou gera outro quando o header está ausente ou inválido. O identificador é devolvido na
resposta, inserido no MDC durante a requisição e incluído nas respostas `ProblemDetail` tratadas.

O back-end registra um evento ao final da requisição com método, caminho sem query string, status e
duração. A saída do console usa JSON no formato Logstash, adequado à coleta futura pelo CloudWatch.

## Segurança e privacidade

Valores externos inválidos nunca entram no MDC, evitando quebra ou falsificação de linhas de log.
Não registrar corpo da requisição, senha, JWT, e-mail completo ou `Idempotency-Key`. O caminho
registrado não inclui query parameters. O identificador serve para correlação, não para autenticação.
O nível padrão é `INFO`; detalhes de requisições do Spring MVC e SQL do Hibernate não são emitidos
em `DEBUG` na configuração normal.

## Consequências

Uma falha informada pelo usuário pode ser localizada nos logs pelo `correlationId`, e cada evento
possui campos pesquisáveis. Há pequeno custo por requisição para geração do UUID, MDC e medição de
tempo. Métricas, traces distribuídos e envio ao CloudWatch serão evoluções posteriores.
