import { useCallback, useEffect, useState } from 'react'
import { getUserBalances } from '../api/splitwise'
import { useAuth } from '../context/AuthContext'
import type { GroupBalanceData } from '../types'

export function useGroupBalances(groupName: string | null) {
  const { handleUnauthorized } = useAuth()
  const [balances, setBalances] = useState<GroupBalanceData | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!groupName) return
    setLoading(true)
    setError(null)
    const result = await getUserBalances({ groupName })
    if (result.status === 401) { handleUnauthorized(result.body); return }
    if (result.ok && result.body?.data) {
      setBalances(result.body.data as GroupBalanceData)
    } else {
      setError('Failed to load balances')
    }
    setLoading(false)
  }, [groupName, handleUnauthorized])

  useEffect(() => { load() }, [load])

  return { balances, loading, error, reload: load }
}
