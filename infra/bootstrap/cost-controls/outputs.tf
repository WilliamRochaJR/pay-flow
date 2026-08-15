output "budget_name" {
  description = "Account-level AWS Budget name."
  value       = aws_budgets_budget.monthly_cost.name
}
