output "state_bucket_name" {
  description = "Bucket name to place in the production backend.hcl file."
  value       = aws_s3_bucket.terraform_state.id
}
