import type { ApiBody } from '../types'

export function extractMessage(responseBody: ApiBody | null | undefined, fallbackMessage: string): string {
  const validationErrors =
    responseBody?.data && typeof responseBody.data === 'object'
      ? Object.values(responseBody.data as Record<string, unknown>)
          .filter((v): v is string => typeof v === 'string')
          .join(' | ')
      : ''
  if (validationErrors) return validationErrors
  return responseBody?.responseMessage ?? fallbackMessage
}
