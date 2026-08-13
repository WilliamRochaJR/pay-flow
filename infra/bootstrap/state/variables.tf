variable "aws_region" {
  description = "AWS region that stores the Terraform state."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Lowercase project name used in the state bucket name."
  type        = string
  default     = "payflow"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,20}$", var.project_name))
    error_message = "project_name must contain 3-21 lowercase letters, numbers or hyphens."
  }
}
