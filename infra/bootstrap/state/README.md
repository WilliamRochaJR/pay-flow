# Bootstrap do state

Este módulo cria o bucket S3 que armazena o state dos demais módulos. Ele é executado uma vez com
state local porque um backend não pode armazenar seu próprio state antes de existir.

```bash
terraform init
terraform plan -out=bootstrap.tfplan
terraform apply bootstrap.tfplan
terraform output -raw state_bucket_name
```

Depois da criação, copie o nome retornado para `environments/production/backend.hcl`. Preserve o
state local do bootstrap em armazenamento seguro até migrá-lo para o bucket criado. Nunca faça
commit de state, planos ou do arquivo real `backend.hcl`.

## Migrar o próprio state para o S3

Depois do primeiro apply, copie `backend.hcl.example` para `backend.hcl`, informe o bucket criado e
execute:

```bash
terraform init -migrate-state -backend-config=backend.hcl
terraform state list
terraform plan
```

`-migrate-state` transfere o state existente para o backend configurado. O plano seguinte deve
informar que não existem mudanças. O backend usa um arquivo de lock no S3 para impedir operações
concorrentes.

O bucket usa criptografia padrão do S3, versionamento, bloqueio completo de acesso público e
`prevent_destroy`. Excluir esse recurso exige uma mudança consciente no código e tratamento das
versões armazenadas.
