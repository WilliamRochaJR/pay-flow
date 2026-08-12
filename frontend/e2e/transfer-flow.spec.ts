import { expect, test } from '@playwright/test'

test('completes a transfer through the published application', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Seu dinheiro, em movimento.' })).toBeVisible()
  await expect(page.getByText('3 contas')).toBeVisible()

  const source = page.getByLabel('Conta de origem')
  const destination = page.getByLabel('Conta de destino')

  await expect(source).toHaveValue('5b99802c-24c0-4462-8260-6317a984da20')
  await expect(destination).toHaveValue('565620a5-e66d-48c9-8ff2-39aa22ace194')

  const createResponse = page.waitForResponse(
    (response) =>
      response.url().endsWith('/api/v1/transfers') &&
      response.request().method() === 'POST' &&
      response.status() === 201,
  )

  await page.getByLabel('Valor').fill('1.00')
  await page.getByRole('button', { name: /transferir agora/i }).click()
  await createResponse

  await expect(page.getByRole('status')).toHaveText('Transferência concluída com sucesso.')
  await expect(page.getByText('Ana Lima → Bruno Costa').first()).toBeVisible()

  await page.reload()

  await expect(page.getByText('Ana Lima → Bruno Costa').first()).toBeVisible()
})
