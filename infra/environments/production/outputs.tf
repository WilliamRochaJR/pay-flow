output "instance_id" {
  description = "EC2 instance ID used by Systems Manager and deployment automation."
  value       = aws_instance.app.id
}

output "public_ip" {
  description = "Elastic IP to configure in DNS."
  value       = aws_eip.app.public_ip
}

output "backup_bucket_name" {
  description = "Private bucket where PostgreSQL backups must be uploaded."
  value       = aws_s3_bucket.backups.id
}

output "ssm_start_session_command" {
  description = "Administrative access command that does not require inbound SSH."
  value       = "aws ssm start-session --target ${aws_instance.app.id} --region ${var.aws_region}"
}
