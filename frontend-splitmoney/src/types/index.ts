// ── HTTP / API ────────────────────────────────────────────────────────────────

export interface ApiResult<T = unknown> {
  ok: boolean
  status: number
  body: ApiBody<T> | null
}

export interface ApiBody<T = unknown> {
  success: boolean
  responseCode?: string
  responseMessage?: string
  requestId?: string
  timestamp?: string
  data?: T
}

// ── Auth ──────────────────────────────────────────────────────────────────────

export interface Session {
  email: string
  userId: string | null
  roles: string[]
  accessTokenExpiresAt: string | null
}

export interface AuthContextValue {
  session: Session | null
  setSession: React.Dispatch<React.SetStateAction<Session | null>>
  applySession: (data: Partial<Session>) => void
  isBootstrappingSession: boolean
  remainingSeconds: number | null
  handleUnauthorized: (body: ApiBody | null) => void
  logout: () => Promise<void>
  isAuthenticated: boolean
}

// ── Groups ────────────────────────────────────────────────────────────────────

export interface Group {
  groupId: number
  groupName: string
  currency: string
  description?: string
  memberCount?: number
}

export interface GroupMember {
  email: string
  role?: string
  name?: string
}

export interface GroupDetails {
  groupId: number
  groupName: string
  currency: string
  description?: string
  members: GroupMember[]
  expenses: Expense[]
  adminEmail?: string
}

// ── Expenses ──────────────────────────────────────────────────────────────────

export type SplitType = 'EQUAL' | 'EXACT' | 'PERCENTAGE'

export interface Expense {
  expenseId: number
  groupId: number
  paidByEmail: string
  totalAmount: string
  currency: string
  description: string
  splitType: SplitType
  expenseDate?: string
  createdAt?: string
  splits?: ExpenseSplit[]
}

export interface ExpenseSplit {
  email: string
  shareAmount: string
}

export interface ExpenseParticipant {
  email: string
  exact_amount?: string
  percentage?: string
}

// ── Balances ──────────────────────────────────────────────────────────────────

export interface BalanceEntry {
  email: string
  amount: string
}

export interface MemberBalance {
  email: string
  amount: string
  isPositive: boolean
}

export interface GroupBalanceData {
  groupName?: string
  userEmail?: string
  currency?: string
  totalToReceive?: string
  totalToPay?: string
  membersWhoNeedToPayUser?: BalanceEntry[]
  membersUserNeedsToPay?: BalanceEntry[]
  membersWhoNeedToPayUserCount?: number
  membersUserNeedsToPayCount?: number
}

// ── Settlement ────────────────────────────────────────────────────────────────

export interface PaymentInitiationResponse {
  saga_id: string
  payment_id: string
  checkout_url: string | null
  status: string
  group_id: number
  paid_by_email: string
  paid_to_email: string
  amount: string
  currency: string
  payment_method: string
}

// ── Notifications ─────────────────────────────────────────────────────────────

export interface NotificationItem {
  id: string
  eventType: string
  title: string
  message: string
  isRead: boolean
  createdAt: string
  date: string
  dateKey: string
  time: string
  payload: Record<string, unknown>
}

export interface NotificationContextValue {
  notifications: NotificationItem[]
  unreadCount: number
  isLoading: boolean
  isFetchingMore: boolean
  hasMore: boolean
  isPanelOpen: boolean
  openPanel: () => void
  closePanel: () => void
  togglePanel: () => void
  loadMore: () => Promise<void>
  markRead: (id: string) => Promise<void>
  markAllRead: () => Promise<void>
}
