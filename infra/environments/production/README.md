# Ambiente production

## O que este módulo cria

- VPC e subnet pública sem NAT Gateway;
- Security Group com somente HTTP e HTTPS de entrada;
- EC2 Ubuntu com Docker, Compose, IMDSv2 obrigatório e administração por Systems Manager;
- Elastic IP para DNS estável;
- bucket S3 privado, versionado, criptografado e com retenção para backups;
- role da EC2 limitada a SSM e ao bucket de backup.

PostgreSQL e API não terão portas públicas. Isso será garantido pelo Compose de produção na próxima
etapa.

## Antes de executar

1. Ative MFA na conta AWS e configure um alerta de orçamento.
2. Confirme preços, Free Tier e quotas atuais da região escolhida.
3. Crie o bucket S3 dedicado ao estado usando `infra/bootstrap/state`.
4. Copie `backend.hcl.example` para `backend.hcl` e use o nome real do bucket.
5. Copie `terraform.tfvars.example` para `terraform.tfvars` e revise os valores.
6. Autentique a CLI por AWS IAM Identity Center ou outra credencial temporária.

`backend.hcl` e `terraform.tfvars` são locais e ignorados pelo Git. Não coloque secrets em arquivos
`*.tf` ou em outputs.

## Validar sem criar recursos

```bash
cd infra/environments/production
terraform fmt -check -recursive
terraform init -backend=false
terraform validate
```

## Planejar

Depois que o backend remoto existir:

```bash
terraform init -backend-config=backend.hcl
terraform plan -out=production.tfplan
terraform show production.tfplan
```

Revise recursos, região, tamanho da EC2 e custos antes de considerar `apply`. O arquivo
`production.tfplan` é local e não deve ser versionado.

## Aplicar

`terraform apply production.tfplan` cria recursos com custo. Ele só deve ser executado depois de uma
revisão explícita do plano. A CI deste Pull Request executa apenas `fmt` e `validate`.

## Estado e recuperação

O backend S3 usa `use_lockfile = true` para impedir operações concorrentes. Versionamento deve estar
habilitado no bucket de state. O bucket de state não é o mesmo bucket criado por este módulo para
backups do PostgreSQL, pois ele precisa existir antes de `terraform init`.

O volume raiz da EC2 preserva dados quando containers são recriados, mas não é um backup e pode ser
substituído junto com a instância. Antes do primeiro deploy público será implementado `pg_dump` para o
bucket privado e um teste de restauração.
