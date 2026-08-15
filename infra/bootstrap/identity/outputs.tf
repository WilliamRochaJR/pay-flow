output "github_oidc_provider_arn" {
  description = "Account-level GitHub Actions OIDC provider ARN used by this module."
  value       = local.github_oidc_provider_arn
}

output "github_deploy_role_arn" {
  description = "Role ARN to store as the non-secret GitHub production environment variable AWS_DEPLOY_ROLE_ARN."
  value       = aws_iam_role.github_deploy.arn
}

output "github_infrastructure_role_arn" {
  description = "Role ARN to store as GitHub production variable AWS_INFRASTRUCTURE_ROLE_ARN."
  value       = aws_iam_role.github_infrastructure.arn
}
