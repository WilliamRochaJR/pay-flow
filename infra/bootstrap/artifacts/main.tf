data "aws_caller_identity" "current" {}

locals {
  repository_names = toset([
    "${var.project_name}-api",
    "${var.project_name}-frontend"
  ])
  repository_arns = [for repository in aws_ecr_repository.application : repository.arn]
  github_oidc_subject = format(
    "repo:%s@%d/%s@%d:environment:%s",
    split("/", var.github_repository)[0],
    var.github_owner_id,
    split("/", var.github_repository)[1],
    var.github_repository_id,
    var.github_environment
  )
}

resource "aws_ecr_repository" "application" {
  for_each = local.repository_names

  name                 = each.value
  image_tag_mutability = "IMMUTABLE"

  encryption_configuration {
    encryption_type = "AES256"
  }

  image_scanning_configuration {
    scan_on_push = true
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_ecr_lifecycle_policy" "application" {
  for_each = aws_ecr_repository.application

  repository = each.value.name
  policy = jsonencode({
    rules = [
      {
        rulePriority = 1
        description  = "Expire untagged images after ${var.untagged_retention_days} days"
        selection = {
          tagStatus   = "untagged"
          countType   = "sinceImagePushed"
          countUnit   = "days"
          countNumber = var.untagged_retention_days
        }
        action = {
          type = "expire"
        }
      }
    ]
  })
}

data "aws_iam_policy_document" "github_publisher_trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [var.github_oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = [local.github_oidc_subject]
    }
  }
}

resource "aws_iam_role" "github_publisher" {
  name                 = "${var.project_name}-artifacts-github-publisher"
  description          = "Temporary GitHub Actions credentials for publishing PayFlow images"
  assume_role_policy   = data.aws_iam_policy_document.github_publisher_trust.json
  max_session_duration = 3600
}

data "aws_iam_policy_document" "github_publisher" {
  statement {
    sid       = "GetEcrAuthorizationToken"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid    = "PublishPayFlowImages"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:DescribeImages",
      "ecr:GetDownloadUrlForLayer",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart"
    ]
    resources = local.repository_arns
  }
}

resource "aws_iam_role_policy" "github_publisher" {
  name   = "${var.project_name}-ecr-publish"
  role   = aws_iam_role.github_publisher.id
  policy = data.aws_iam_policy_document.github_publisher.json
}

data "aws_iam_policy_document" "runtime_pull" {
  statement {
    sid       = "GetEcrAuthorizationToken"
    effect    = "Allow"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid    = "PullPayFlowImages"
    effect = "Allow"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:GetDownloadUrlForLayer"
    ]
    resources = local.repository_arns
  }
}

resource "aws_iam_policy" "runtime_pull" {
  name        = "${var.project_name}-ecr-pull"
  description = "Read-only access to the PayFlow application images"
  policy      = data.aws_iam_policy_document.runtime_pull.json
}
