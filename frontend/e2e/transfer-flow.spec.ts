import { expect, test } from '@playwright/test'

test('completes a transfer through the published application', async ({ page }) => {
  await page.goto('/')
  await expect(page).toHaveURL(/\/login$/)

  const email = `e2e-${Date.now()}@example.com`
  await page.getByRole('link', { name: 'Criar conta' }).click()
  await expect(page).toHaveURL(/\/register$/)
  await page.getByLabel('Nome').fill('E2E User')
  await page.getByLabel('E-mail').fill(email)
  await page.getByLabel('Senha').fill('safe-password')
  await page.getByRole('button', { name: /^criar conta$/i }).click()

  await expect(
    page.getByText('Conta criada com sucesso. Entre com seu e-mail e senha.'),
  ).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Acesse sua conta' })).toBeVisible()
  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByLabel('E-mail')).toHaveValue(email)
  await page.getByLabel('Senha').fill('safe-password')
  await page.getByRole('button', { name: /^entrar$/i }).click()

  await expect(page.getByRole('heading', { name: 'Seu dinheiro, em movimento.' })).toBeVisible()
  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByText('2 contas')).toBeVisible()

  const source = page.getByLabel('Conta de origem')
  const destination = page.getByLabel('Conta de destino')

  await expect(source).not.toHaveValue('')
  await expect(destination).not.toHaveValue('')

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
  await expect(page.getByText(/E2E User • Principal → E2E User • Reserva/).first()).toBeVisible()
})
