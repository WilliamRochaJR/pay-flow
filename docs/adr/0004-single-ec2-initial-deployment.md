# ADR-0004 — Deploy inicial em uma EC2 com Docker Compose

- Status: Aceito
- Data: 2026-08-10
- Atualizado em: 2026-08-13

## Contexto

O primeiro ambiente deve publicar front-end, API e banco na AWS com baixo custo e pouca infraestrutura. Distribuir imediatamente os componentes entre vários serviços aumenta configuração de rede, permissões e cobrança.

## Decisão

Executar Caddy, front-end, API e PostgreSQL em containers numa única EC2. Usar volume persistente,
backup automatizado fora da instância, HTTPS, health checks e deploy reproduzível.

Provisionar a fundação AWS do M1 com Terraform: rede pública dedicada, regras de entrada HTTP/HTTPS,
EC2, Elastic IP, identidade da instância e bucket privado de backup. O estado será configurado por um
backend S3 externo ao módulo, e `terraform apply` permanecerá explícito e protegido.

O código Terraform não provisiona EKS, RDS, Kafka, NAT Gateway ou múltiplas instâncias. O deploy da
aplicação será uma etapa separada e ocorrerá apenas com uma revisão confiável da `main`.

## Consequências

O ambiente é barato e fácil de compreender, mas possui um único ponto de falha e escala verticalmente.
Terraform melhora a reprodutibilidade, porém introduz estado remoto, revisão de planos e necessidade de
controlar permissões AWS. O bucket de backup não substitui testes periódicos de restauração.

Quando disponibilidade ou operação justificarem, PostgreSQL migra para RDS, o front para CDN e a API
para uma plataforma com múltiplas instâncias.

Para a fase de demonstração do portfólio, o ciclo de vida temporário, o acesso sem domínio e o TTL são
definidos pelo [ADR-0019](0019-ephemeral-aws-poc-with-ttl.md).
