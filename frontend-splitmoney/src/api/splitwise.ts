import { requestJson } from './http'
import { getRequestMeta, generateId } from '../utils/requestMeta'
import type { ApiResult, GroupDetails, GroupBalanceData, Group } from '../types'

const SW_BASE = '/api/splitwise'

function sw<T = unknown>(path: string, method: string, payload?: unknown, extraHeaders: Record<string, string> = {}): Promise<ApiResult<T>> {
  return requestJson<T>(SW_BASE, path, method, payload, extraHeaders)
}

export function getUserGroups(): Promise<ApiResult<{ groups: Group[] }>> {
  return sw<{ groups: Group[] }>('/v1/user-groups', 'POST', getRequestMeta())
}

export function createGroup({ name, description, currency, members }: {
  name: string; description?: string; currency: string; members: string[]
}) {
  return sw('/v1/create-group', 'POST', {
    ...getRequestMeta(), name, description: description ?? '', currency, members,
  })
}

export function getGroupDetails({ groupName }: { groupName: string }): Promise<ApiResult<GroupDetails>> {
  return sw<GroupDetails>('/v1/group-details', 'POST', { ...getRequestMeta(), groupName })
}

export function getUserBalances({ groupName }: { groupName: string }): Promise<ApiResult<GroupBalanceData>> {
  return sw<GroupBalanceData>('/v1/user-balances', 'POST', { ...getRequestMeta(), groupName })
}

export function addMember({ groupName, memberEmail, memberPhone }: {
  groupName: string; memberEmail: string; memberPhone?: string
}) {
  return sw('/v1/add-member', 'PUT', {
    ...getRequestMeta(), groupName,
    memberToAdd: { email: memberEmail, phoneNumber: memberPhone ?? null },
  })
}

export function removeMember({ groupName, memberEmail }: { groupName: string; memberEmail: string }) {
  return sw('/v1/remove-member', 'DELETE', {
    ...getRequestMeta(), groupName, memberToRemove: { email: memberEmail },
  })
}

export function addExpense({ groupId, paidByEmail, totalAmount, currency, description, expenseDate, splitType, participants, idempotencyKey }: {
  groupId: number; paidByEmail: string; totalAmount: string | number; currency: string
  description: string; expenseDate?: string | null; splitType: string
  participants: Array<{ email: string; exact_amount?: string; percentage?: string }>
  idempotencyKey?: string
}) {
  const headers: Record<string, string> = idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
  return requestJson('/api/v1/expenses', '', 'POST', {
    ...getRequestMeta(),
    group_id: groupId, paid_by_email: paidByEmail, total_amount: String(totalAmount),
    currency, description, expense_date: expenseDate ?? null, split_type: splitType, participants,
  }, headers)
}

export function settleExpense({ groupId, paidByEmail, paidTo, amount, currency, paymentMethod, returnUrl, idempotencyKey }: {
  groupId: number; paidByEmail: string; paidTo: string; amount: string
  currency: string; paymentMethod: 'CARD' | 'UPI'; returnUrl: string; idempotencyKey?: string
}) {
  const headers: Record<string, string> = idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
  return requestJson('/api/v1/expenses', '/settle/initiate', 'POST', {
    ...getRequestMeta(),
    group_id: groupId, paid_by_email: paidByEmail, paid_to: paidTo,
    amount: String(amount), currency, payment_method: paymentMethod, return_url: returnUrl,
  }, headers)
}
