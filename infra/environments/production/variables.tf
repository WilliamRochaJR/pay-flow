variable "aws_region" {
  description = "AWS region where the PayFlow environment will be created."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Lowercase name used in resource names and tags."
  type        = string
  default     = "payflow"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,20}$", var.project_name))
    error_message = "project_name must contain 3-21 lowercase letters, numbers or hyphens."
  }
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "production"
}

variable "instance_type" {
  description = "EC2 instance type. Confirm current pricing and Free Tier eligibility before applying."
  type        = string
  default     = "t3.micro"
}

variable "root_volume_size_gib" {
  description = "Encrypted gp3 root volume size in GiB."
  type        = number
  default     = 20

  validation {
    condition     = var.root_volume_size_gib >= 16 && var.root_volume_size_gib <= 100
    error_message = "root_volume_size_gib must be between 16 and 100 GiB."
  }
}

variable "http_ingress_cidrs" {
  description = "IPv4 CIDRs allowed to reach public HTTP and HTTPS endpoints."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "backup_retention_days" {
  description = "Number of days before database backup objects expire."
  type        = number
  default     = 30

  validation {
    condition     = var.backup_retention_days >= 7 && var.backup_retention_days <= 365
    error_message = "backup_retention_days must be between 7 and 365."
  }
}
