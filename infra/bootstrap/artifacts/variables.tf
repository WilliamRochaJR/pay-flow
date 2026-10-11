variable "aws_region" {
  description = "AWS region that stores the container images."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Lowercase project name used in artifact resource names."
  type        = string
  default     = "payflow"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,20}$", var.project_name))
    error_message = "project_name must contain 3-21 lowercase letters, numbers or hyphens."
  }
}

variable "github_repository" {
  description = "Repository allowed to publish PayFlow images."
  type        = string
  default     = "WilliamRochaJR/pay-flow"

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", var.github_repository))
    error_message = "github_repository must use the owner/repository format."
  }
}

variable "github_owner_id" {
  description = "Immutable numeric GitHub owner ID included in the customized OIDC subject."
  type        = number
  default     = 38361127

  validation {
    condition     = var.github_owner_id > 0
    error_message = "github_owner_id must be a positive GitHub numeric ID."
  }
}

variable "github_repository_id" {
  description = "Immutable numeric GitHub repository ID included in the customized OIDC subject."
  type        = number
  default     = 1331549119

  validation {
    condition     = var.github_repository_id > 0
    error_message = "github_repository_id must be a positive GitHub numeric ID."
  }
}

variable "github_environment" {
  description = "GitHub Environment trusted to publish immutable artifacts."
  type        = string
  default     = "artifacts"

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]{1,64}$", var.github_environment))
    error_message = "github_environment must contain only letters, numbers, dots, underscores or hyphens."
  }
}

variable "github_oidc_provider_arn" {
  description = "Existing account-level GitHub Actions OIDC provider ARN."
  type        = string

  validation {
    condition     = can(regex("^arn:aws:iam::[0-9]{12}:oidc-provider/token\\.actions\\.githubusercontent\\.com$", var.github_oidc_provider_arn))
    error_message = "github_oidc_provider_arn must identify the account GitHub Actions provider."
  }
}

variable "untagged_retention_days" {
  description = "Days to retain untagged images before ECR removes them."
  type        = number
  default     = 7

  validation {
    condition     = var.untagged_retention_days >= 1 && var.untagged_retention_days <= 30
    error_message = "untagged_retention_days must be between 1 and 30."
  }
}
