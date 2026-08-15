variable "aws_region" {
  description = "AWS region used by the provider. AWS Budgets itself is account-level."
  type        = string
  default     = "us-east-1"
}

variable "monthly_budget_usd" {
  description = "Monthly AWS cost budget in US dollars. This creates alerts, not a hard spending limit."
  type        = number
  default     = 5

  validation {
    condition     = var.monthly_budget_usd > 0
    error_message = "monthly_budget_usd must be greater than zero."
  }
}

variable "alert_email" {
  description = "Email that receives AWS Budget notifications. Supply it through TF_VAR_alert_email."
  type        = string
  sensitive   = true

  validation {
    condition     = can(regex("^[^@[:space:]]+@[^@[:space:]]+\\.[^@[:space:]]+$", var.alert_email))
    error_message = "alert_email must be a valid email address."
  }
}
