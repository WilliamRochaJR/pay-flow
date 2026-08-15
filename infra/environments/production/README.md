# Ambiente production

## O que este módulo cria

- VPC e subnet pública sem NAT Gateway;
- Security Group com somente HTTP de entrada enquanto não houver domínio;
- EC2 Ubuntu com Docker, Compose, IMDSv2 obrigatório e administração por Systems Manager;
- Elastic IP para DNS estável;
- bucket S3 privado, versionado, criptografado e com retenção para backups;
- role da EC2 limitada a SSM e ao bucket de backup.

PostgreSQL e API não terão portas públicas. Isso será garantido pelo Compose de produção na próxima
etapa.

Sem domínio, a PoC usa o output `public_url` em HTTP. Caddy continua como proxy reverso, mas não tenta
emitir um certificado para um endereço IP. A porta 443 será adicionada somente junto com a adoção de
domínio e HTTPS.

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

Na operação normal, o workflow manual `AWS Ephemeral PoC` executa `plan`, `apply`, deploy, health check
e `destroy`. Escolha um TTL de 20, 40 ou 60 minutos. Não execute `apply` local ao mesmo tempo, pois os
dois processos compartilham o mesmo state e lock.

O workflow `AWS Ephemeral Cleanup` roda periodicamente e também aceita acionamento manual. Ele destrói
o ambiente somente quando a concessão no S3 expirou; no acionamento manual, força a limpeza. Os
workflows nunca são disparados por um Pull Request.

Configure no GitHub Environment `production`:

| Tipo     | Nome                          | Conteúdo                                |
| -------- | ----------------------------- | --------------------------------------- |
| Variable | `AWS_STATE_BUCKET`            | Nome do bucket privado de state         |
| Variable | `AWS_INFRASTRUCTURE_ROLE_ARN` | Output da role Terraform                |
| Variable | `AWS_DEPLOY_ROLE_ARN`         | Output da role SSM                      |
| Secret   | `POSTGRES_PASSWORD`           | Senha exclusiva e aleatória da PoC      |
| Secret   | `JWT_SECRET`                  | Segredo Base64 forte e exclusivo da PoC |

Restrinja as deployment branches desse Environment à `main`. Enquanto o watchdog usar o mesmo
Environment, não exija aprovação manual nele, pois jobs agendados ficariam aguardando aprovação e não
destruiriam o ambiente expirado. O job mostra a URL e o horário de expiração no Summary da execução.

## Estado e recuperação

O backend S3 usa `use_lockfile = true` para impedir operações concorrentes. Versionamento deve estar
habilitado no bucket de state. O bucket de state não é o mesmo bucket criado por este módulo para
backups do PostgreSQL, pois ele precisa existir antes de `terraform init`.

O volume raiz da EC2 preserva dados quando containers são recriados, mas não é um backup e pode ser
substituído junto com a instância. Antes do primeiro deploy público será implementado `pg_dump` para o
bucket privado e um teste de restauração.
