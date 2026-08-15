# ADR-0018 — GitHub Actions autentica na AWS com OIDC

- Status: Aceito
- Data: 2026-08-14

## Contexto

O deploy do M1 precisa executar ações na EC2 sem armazenar access key e secret access key de longa
duração no GitHub. A conta AWS já pode possuir um provider OIDC criado por outro projeto, e providers
para a mesma URL devem ser compartilhados no nível da conta.

## Decisão

Usar OpenID Connect (OIDC) para que o GitHub Actions receba credenciais AWS temporárias. A trust
policy aceitará somente tokens do repositório `WilliamRochaJR/pay-flow`, com audiência
`sts.amazonaws.com` e subject do GitHub Environment `production`.

Criar uma role exclusiva de deploy do PayFlow. Inicialmente ela poderá somente enviar e consultar
comandos do AWS Systems Manager para instâncias marcadas com `Project=payflow` e
`Environment=production`. A role não terá `AdministratorAccess`, acesso ao state do Terraform ou
permissões de provisionamento.

O módulo aceitará o ARN de um provider GitHub OIDC já existente ou criará o provider quando ele não
existir. O primeiro provisionamento da infraestrutura continuará manual, autenticado por SSO. Uma
role separada para Terraform só será criada se a automação de `plan/apply` for adotada posteriormente.

## Consequências

Não existem chaves AWS permanentes no GitHub. Cada job recebe credenciais temporárias e somente após
passar pelas regras do Environment `production`.

A role de deploy pode executar comandos na EC2 selecionada e, portanto, continua sendo privilegiada
dentro dessa instância. O Environment deve restringir branches, exigir aprovação quando configurado e
ser usado apenas por workflows revisados na `main`.

Reutilizar um provider existente evita conflito na conta, mas exige descobrir e informar seu ARN antes
do primeiro `plan`.
