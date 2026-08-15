data "aws_caller_identity" "current" {}

resource "aws_iam_openid_connect_provider" "github" {
  count = var.existing_github_oidc_provider_arn == null ? 1 : 0

  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]

  tags = { Name = "github-actions" }
}

locals {
  name = "${var.project_name}-${var.environment}"
  github_oidc_provider_arn = var.existing_github_oidc_provider_arn != null ? (
    var.existing_github_oidc_provider_arn
  ) : aws_iam_openid_connect_provider.github[0].arn
}

data "aws_iam_policy_document" "github_deploy_trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.github_oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repository}:environment:${var.environment}"]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name                 = "${local.name}-github-deploy"
  description          = "Temporary GitHub Actions credentials for PayFlow deployment through SSM"
  assume_role_policy   = data.aws_iam_policy_document.github_deploy_trust.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_deploy" {
  statement {
    sid     = "UseApprovedRunShellScriptDocument"
    effect  = "Allow"
    actions = ["ssm:SendCommand"]
    resources = [
      "arn:aws:ssm:${var.aws_region}::document/AWS-RunShellScript"
    ]
  }

  statement {
    sid     = "RunCommandsOnlyOnPayFlowProduction"
    effect  = "Allow"
    actions = ["ssm:SendCommand"]
    resources = [
      "arn:aws:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/*"
    ]

    condition {
      test     = "StringEquals"
      variable = "ssm:resourceTag/Project"
      values   = [var.project_name]
    }

    condition {
      test     = "StringEquals"
      variable = "ssm:resourceTag/Environment"
      values   = [var.environment]
    }
  }

  statement {
    sid    = "ReadCommandResults"
    effect = "Allow"
    actions = [
      "ssm:GetCommandInvocation",
      "ssm:ListCommandInvocations",
      "ssm:ListCommands"
    ]
    resources = ["*"]
  }

  statement {
    sid       = "InspectManagedInstances"
    effect    = "Allow"
    actions   = ["ssm:DescribeInstanceInformation"]
    resources = ["*"]
  }

  statement {
    sid    = "ManageEncryptedRuntimeConfiguration"
    effect = "Allow"
    actions = [
      "ssm:DeleteParameter",
      "ssm:PutParameter"
    ]
    resources = [
      "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/${var.project_name}/${var.environment}/runtime-env"
    ]
  }

  statement {
    sid = "ManageEphemeralLease"
    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject"
    ]
    resources = [
      "arn:aws:s3:::${var.terraform_state_bucket_name}/payflow/leases/*"
    ]
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "${local.name}-ssm-deploy"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}

resource "aws_iam_role" "github_infrastructure" {
  name                 = "${local.name}-github-infrastructure"
  description          = "Temporary GitHub Actions credentials for PayFlow Terraform apply and destroy"
  assume_role_policy   = data.aws_iam_policy_document.github_deploy_trust.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_infrastructure" {
  statement {
    sid = "ReadAccountAndAmiMetadata"
    actions = [
      "ec2:Describe*",
      "ssm:GetParameter"
    ]
    resources = ["*"]
  }

  statement {
    sid = "ManageEphemeralNetworkAndCompute"
    actions = [
      "ec2:AllocateAddress",
      "ec2:AssociateAddress",
      "ec2:AssociateRouteTable",
      "ec2:AttachInternetGateway",
      "ec2:AuthorizeSecurityGroupEgress",
      "ec2:AuthorizeSecurityGroupIngress",
      "ec2:CreateInternetGateway",
      "ec2:CreateRoute",
      "ec2:CreateRouteTable",
      "ec2:CreateSecurityGroup",
      "ec2:CreateSubnet",
      "ec2:CreateTags",
      "ec2:CreateVpc",
      "ec2:DeleteInternetGateway",
      "ec2:DeleteRoute",
      "ec2:DeleteRouteTable",
      "ec2:DeleteSecurityGroup",
      "ec2:DeleteSubnet",
      "ec2:DeleteTags",
      "ec2:DeleteVpc",
      "ec2:DetachInternetGateway",
      "ec2:DisassociateAddress",
      "ec2:DisassociateRouteTable",
      "ec2:ModifySubnetAttribute",
      "ec2:ModifyVpcAttribute",
      "ec2:ReleaseAddress",
      "ec2:RevokeSecurityGroupEgress",
      "ec2:RevokeSecurityGroupIngress",
      "ec2:RunInstances",
      "ec2:TerminateInstances"
    ]
    resources = ["*"]
  }

  statement {
    sid = "ManagePayFlowInstanceIdentity"
    actions = [
      "iam:AddRoleToInstanceProfile",
      "iam:AttachRolePolicy",
      "iam:CreateInstanceProfile",
      "iam:CreateRole",
      "iam:DeleteInstanceProfile",
      "iam:DeleteRole",
      "iam:DeleteRolePolicy",
      "iam:DetachRolePolicy",
      "iam:GetInstanceProfile",
      "iam:GetRole",
      "iam:GetRolePolicy",
      "iam:ListAttachedRolePolicies",
      "iam:ListInstanceProfilesForRole",
      "iam:ListRolePolicies",
      "iam:PassRole",
      "iam:PutRolePolicy",
      "iam:RemoveRoleFromInstanceProfile",
      "iam:TagInstanceProfile",
      "iam:TagRole"
    ]
    resources = [
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${local.name}-*",
      "arn:aws:iam::${data.aws_caller_identity.current.account_id}:instance-profile/${local.name}-*"
    ]
  }

  statement {
    sid = "ReadTerraformStateBucket"
    actions = [
      "s3:GetBucketLocation",
      "s3:ListBucket"
    ]
    resources = ["arn:aws:s3:::${var.terraform_state_bucket_name}"]
  }

  statement {
    sid = "ManageProductionStateAndLease"
    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject"
    ]
    resources = [
      "arn:aws:s3:::${var.terraform_state_bucket_name}/payflow/production/*",
      "arn:aws:s3:::${var.terraform_state_bucket_name}/payflow/leases/*"
    ]
  }

  statement {
    sid = "ManageEphemeralBackupBucket"
    actions = [
      "s3:CreateBucket",
      "s3:DeleteBucket",
      "s3:DeleteBucketPolicy",
      "s3:DeleteObject",
      "s3:DeleteObjectVersion",
      "s3:Get*",
      "s3:ListBucket",
      "s3:ListBucketVersions",
      "s3:PutBucketPublicAccessBlock",
      "s3:PutBucketTagging",
      "s3:PutBucketVersioning",
      "s3:PutEncryptionConfiguration",
      "s3:PutLifecycleConfiguration"
    ]
    resources = [
      "arn:aws:s3:::${local.name}-${data.aws_caller_identity.current.account_id}-${var.aws_region}-backups",
      "arn:aws:s3:::${local.name}-${data.aws_caller_identity.current.account_id}-${var.aws_region}-backups/*"
    ]
  }
}

resource "aws_iam_role_policy" "github_infrastructure" {
  name   = "${local.name}-terraform-ephemeral"
  role   = aws_iam_role.github_infrastructure.id
  policy = data.aws_iam_policy_document.github_infrastructure.json
}
