export type Transfer = {
  id: string
  type: 'INTERNAL_TRANSFER' | 'REVERSAL'
  originalTransferId: string | null
  sourceAccountId: string
  destinationAccountId: string
  amount: number
  currency: string
  status: 'COMPLETED'
  createdAt: string
}

export type TransferFilters = {
  from?: string
  to?: string
}

export type TransferPage = {
  content: Transfer[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export const emptyTransferPage: TransferPage = {
  content: [],
  page: 0,
  size: 5,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
}
