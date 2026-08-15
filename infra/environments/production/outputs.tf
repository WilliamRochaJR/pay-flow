output "instance_id" {
  description = "EC2 instance ID used by Systems Manager and deployment automation."
  value       = aws_instance.app.id
}

output "public_ip" {
  description = "Temporary Elastic IP used to access the domainless PoC over HTTP."
  value       = aws_eip.app.public_ip
}

output "public_url" {
  description = "Temporary HTTP URL. It changes after the environment is destroyed and recreated."
  value       = "http://${aws_eip.app.public_ip}"
}

output "backup_bucket_name" {
  description = "Private bucket where PostgreSQL backups must be uploaded."
  value       = aws_s3_bucket.backups.id
}

output "ssm_start_session_command" {
  description = "Administrative access command that does not require inbound SSH."
  value       = "aws ssm start-session --target ${aws_instance.app.id} --region ${var.aws_region}"
}
