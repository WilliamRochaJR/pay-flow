# ADR-0004 — Deploy inicial em uma EC2 com Docker Compose

- Status: Aceito
- Data: 2026-08-10

## Contexto

O primeiro ambiente deve publicar front-end, API e banco na AWS com baixo custo e pouca infraestrutura. Distribuir imediatamente os componentes entre vários serviços aumenta configuração de rede, permissões e cobrança.

## Decisão

Executar Caddy, front-end, API e PostgreSQL em containers numa única EC2. Usar volume persistente, backup automatizado fora da instância, HTTPS, health checks e deploy reproduzível.

## Consequências

O ambiente é barato e fácil de compreender, mas possui um único ponto de falha e escala verticalmente. Quando disponibilidade ou operação justificarem, PostgreSQL migra para RDS, o front para CDN e a API para uma plataforma com múltiplas instâncias.
