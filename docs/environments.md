# Ambientes e promoção

Este documento transforma a decisão do ADR-0020 em um plano operacional incremental. Separar nomes
de ambientes no GitHub não é suficiente: cada ambiente AWS precisa isolar configuração, credenciais,
state, lease de TTL e dados.

## Estado atual

| Ambiente       | Origem prevista | GitHub Environment | Infraestrutura AWS | Estado             |
| -------------- | --------------- | ------------------ | ------------------ | ------------------ |
| `development`  | `develop`       | configurado        | efêmera com TTL    | validado           |
| `homologation` | `release/*`     | configurado        | efêmera com TTL    | aceite aprovado    |
| `production`   | tag em `main`   | configurado        | efêmera com TTL    | implementado no M1 |

O workflow publica `develop` em `development`, `release/X.Y.Z` em `homologation` e exige uma tag
anotada `vX.Y.Z` para `production`. O watchdog agendado permanece em produção; os demais ambientes
são destruídos ao final do próprio job e possuem limpeza manual de emergência.

## Fluxo-alvo

```mermaid
flowchart TB
    Feature[feature/*] -->|PR + CI| Develop[develop]
    Develop -->|aprovação manual| Dev[development efêmero]
    Develop -->|cria| Release[release/x.y.z]
    Release -->|aprovação manual| Hom[homologation efêmero]
    Release -->|PR + CI| Main[main]
    Main --> Tag[tag vX.Y.Z]
    Tag -->|aprovação protegida| Prod[production efêmero]

    Dev -.não promove dados.-> Hom
    Hom -.promove a revisão validada.-> Prod
```

Promover significa usar a mesma revisão validada, e não copiar banco, secrets ou estado entre
ambientes. Nesta primeira evolução, a revisão será identificada pelo SHA do commit ou pela tag. A
promoção de uma imagem Docker imutável ficará para uma etapa posterior.

## Isolamento obrigatório

Cada ambiente deverá ter valores próprios para:

- GitHub Environment e suas regras de aprovação;
- roles AWS assumidas pelo GitHub OIDC;
- chave do state Terraform no S3;
- arquivo de lease usado pelo TTL e pela limpeza automática;
- parâmetro `SecureString` do runtime no Systems Manager;
- secrets `POSTGRES_PASSWORD` e `JWT_SECRET`;
- tags AWS, nomes de recursos e dados do PostgreSQL;
- URL pública e valor de CORS.

Nenhum banco, senha, token ou arquivo Terraform state será promovido entre ambientes.

## Ordem de implementação

### 1. Tornar o workflow consciente do ambiente

Extrair os valores hoje fixos em `production` para entradas controladas e mapas explícitos. O workflow
deverá rejeitar combinações inválidas entre revisão e ambiente.

Estado: implementado na política e no workflow para os três ambientes.

### 2. Criar `development`

Adicionar state, identidade, configuração e limpeza exclusivos. A publicação será manual a partir de
`develop`, efêmera e sem aprovação obrigatória inicialmente.

Preparação no repositório:

- reutilizar o root Terraform parametrizado, sem copiar recursos;
- state da aplicação em `payflow/development/terraform.tfstate`;
- state da identidade em `payflow/bootstrap/development-identity.tfstate`;
- roles `payflow-development-*` confiando somente no Environment `development`;
- SSM `/payflow/development/runtime-env` e lease `payflow/leases/development.json`;
- a opção de publicação permaneceu bloqueada até a conclusão do bootstrap e o cadastro de
  secrets/variables.

Estado: concluído e validado em 2026-09-27.

#### Evidência do primeiro ciclo

A execução [GitHub Actions #36289398433](https://github.com/WilliamRochaJR/pay-flow/actions/runs/36289398433)
publicou o commit `85982b1` da branch `develop` com TTL de 20 minutos. O health check público retornou
`UP` antes do início do TTL.

Ao final, todas as etapas de limpeza passaram. A verificação posterior confirmou:

- nenhuma instância EC2 ativa com `Project=payflow` e `Environment=development`;
- nenhum Elastic IP de `development` alocado;
- lease `payflow/leases/development.json` removido;
- parâmetro `/payflow/development/runtime-env` removido;
- workflow concluído com sucesso.

A URL e os dados da aplicação eram efêmeros e não são tratados como artefatos promovíveis.

### 3. Criar `homologation`

Adicionar o mesmo isolamento, mas aceitar somente revisões `release/*`. Esse ambiente serve para o
aceite da versão candidata antes do merge na `main`.

Preparação no repositório:

- reutilizar o mesmo root Terraform parametrizado;
- state da aplicação em `payflow/homologation/terraform.tfstate`;
- state da identidade em `payflow/bootstrap/homologation-identity.tfstate`;
- planejar roles `payflow-homologation-*` confiando somente no Environment `homologation`;
- reservar SSM `/payflow/homologation/runtime-env` e lease `payflow/leases/homologation.json`;
- manter a opção de publicação bloqueada até concluir identidade, Environment e secrets.

Estado: concluído e aprovado funcionalmente em 2026-09-27. A confirmação da destruição automática
será registrada após o encerramento do primeiro TTL.

#### Evidência do primeiro ciclo

A execução [GitHub Actions #36341890422](https://github.com/WilliamRochaJR/pay-flow/actions/runs/36341890422)
publicou o merge commit `944cf34` da branch `release/0.2.0`. O health check público retornou `UP` e o
TTL de 60 minutos só começou depois que a aplicação ficou saudável.

O aceite manual confirmou os fluxos de cadastro, login e transferência. A URL, o banco e os dados
usados nesse teste são efêmeros e não serão promovidos. A promoção para produção usará a revisão
validada, posteriormente identificada pela tag anotada `v0.2.0`.

### 4. Restringir `production`

Depois que os dois ambientes anteriores estiverem comprovados, produção aceitará somente tags
`vX.Y.Z` existentes e poderá exigir um revisor no GitHub Environment.

### 5. Promover artefatos imutáveis

Construir as imagens uma vez, identificá-las por digest e promover exatamente os mesmos artefatos.
Essa etapa evita que builds separados da mesma revisão produzam resultados diferentes, mas não é
necessária para criar os primeiros ambientes isolados.

## Custos e segurança

Criar `development` e `homologation` não significa mantê-los ligados. Todos continuam manuais,
efêmeros e protegidos por TTL. A implementação deve reutilizar módulos Terraform, mas nunca o mesmo
state ou banco. Pull Requests comuns executam apenas validação e não criam recursos AWS.

## Critério de conclusão do primeiro incremento

Os ciclos de `development` e `homologation` foram publicados e validados. O próximo incremento promove
a revisão aceita para `production` por meio da tag anotada `v0.2.0`.
