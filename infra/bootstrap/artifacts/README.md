# Bootstrap dos artefatos

Este root Terraform declara a fundação persistente de imagens do ADR-0026. Ele não pertence ao TTL
dos ambientes de execução e cria:

- os repositórios ECR `payflow-api` e `payflow-frontend`;
- tags imutáveis, criptografia AES-256 e scan no push;
- expiração de imagens sem tag depois de sete dias por padrão;
- uma role OIDC de escrita limitada aos dois repositórios;
- uma managed policy de leitura para as instâncias que executarão as imagens.

O módulo não constrói nem publica imagens. Também não conecta ainda a policy de leitura à EC2; essa
ligação entra junto com o deploy por digest para não alterar o runtime antes de o novo fluxo existir.

## Pré-requisitos

1. o bucket de state do `bootstrap/state` já existe;
2. o provider OIDC da conta já existe e seu ARN pode ser obtido no output de `bootstrap/identity`;
3. existe um GitHub Environment chamado `artifacts` para proteger a publicação;
4. a sessão AWS usada no primeiro apply pode administrar ECR e as roles/policies IAM deste módulo.

Não coloque IDs de conta reais em arquivos versionados. O ARN do provider contém o ID da conta, por
isso deve ficar apenas no `terraform.tfvars` local ou ser informado por `TF_VAR_*`.

## Planejar

```bash
cd infra/bootstrap/artifacts
cp backend.hcl.example backend.hcl
cp terraform.tfvars.example terraform.tfvars
```

Substitua os placeholders locais e execute:

```bash
terraform init -backend-config=backend.hcl
terraform fmt -check
terraform validate
terraform plan -out=artifacts.tfplan
```

Revise o plano antes de solicitar autorização para aplicar. Pull Requests executam apenas
`terraform init -backend=false` e `terraform validate`; eles não criam recursos na AWS.

## Aplicar e configurar o GitHub

Depois de um apply explicitamente autorizado:

```bash
terraform apply artifacts.tfplan
terraform output
```

Configure o output `github_publisher_role_arn` como a variável
`AWS_ARTIFACT_PUBLISHER_ROLE_ARN` do GitHub Environment `artifacts`. Os URLs dos repositórios e a
policy de leitura serão consumidos pelos próximos incrementos do M5.2.

O `prevent_destroy` dos repositórios protege os artefatos contra remoção acidental. Excluir os
repositórios exigirá uma alteração consciente desse código e tratamento prévio das imagens retidas.
