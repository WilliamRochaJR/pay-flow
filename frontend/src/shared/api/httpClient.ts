export type Problem = {
  detail?: string
}

const apiUrl = import.meta.env.VITE_API_URL ?? ''

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiUrl}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options?.headers },
  })

  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as Problem
    throw new Error(problem.detail ?? 'Não foi possível concluir a operação.')
  }

  return response.json() as Promise<T>
}
