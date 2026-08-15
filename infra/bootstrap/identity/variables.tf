variable "aws_region" {
  description = "AWS region used in regional IAM policy resource ARNs."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project tag required on instances managed by the deploy role."
  type        = string
  default     = "payflow"
}

variable "environment" {
  description = "GitHub Environment and AWS resource environment tag."
  type        = string
  default     = "production"
}

variable "github_repository" {
  description = "Repository allowed to request the AWS deploy role."
  type        = string
  default     = "WilliamRochaJR/pay-flow"

  validation {
    condition     = can(regex("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$", var.github_repository))
    error_message = "github_repository must use the owner/repository format."
  }
}

variable "existing_github_oidc_provider_arn" {
  description = "Existing account-level GitHub OIDC provider ARN. Leave null to create it."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition = (
      var.existing_github_oidc_provider_arn == null ||
      can(regex("^arn:aws:iam::[0-9]{12}:oidc-provider/token\\.actions\\.githubusercontent\\.com$", var.existing_github_oidc_provider_arn))
    )
    error_message = "existing_github_oidc_provider_arn must be the account GitHub Actions provider ARN."
  }
}
