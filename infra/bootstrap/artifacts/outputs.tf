output "api_repository_arn" {
  description = "ARN of the ECR repository that stores the API image."
  value       = aws_ecr_repository.application["${var.project_name}-api"].arn
}

output "api_repository_url" {
  description = "URL used to push and pull the API image."
  value       = aws_ecr_repository.application["${var.project_name}-api"].repository_url
}

output "frontend_repository_arn" {
  description = "ARN of the ECR repository that stores the front-end image."
  value       = aws_ecr_repository.application["${var.project_name}-frontend"].arn
}

output "frontend_repository_url" {
  description = "URL used to push and pull the front-end image."
  value       = aws_ecr_repository.application["${var.project_name}-frontend"].repository_url
}

output "github_oidc_subject" {
  description = "Exact GitHub OIDC subject trusted by the artifact publisher role."
  value       = local.github_oidc_subject
}

output "github_publisher_role_arn" {
  description = "Role ARN to configure as AWS_ARTIFACT_PUBLISHER_ROLE_ARN."
  value       = aws_iam_role.github_publisher.arn
}

output "runtime_pull_policy_arn" {
  description = "Managed policy ARN to attach to runtime instance roles."
  value       = aws_iam_policy.runtime_pull.arn
}
