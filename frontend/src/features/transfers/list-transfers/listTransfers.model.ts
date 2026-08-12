export type Transfer = {
  id: string
  type: 'INTERNAL_TRANSFER'
  sourceAccountId: string
  destinationAccountId: string
  amount: number
  currency: string
  status: 'COMPLETED'
  createdAt: string
}
