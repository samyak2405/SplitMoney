import { useCallback, useEffect, useState } from 'react'
import { getUserGroups } from '../api/splitwise'
import { useAuth } from '../context/AuthContext'
import type { Group } from '../types'

export function useGroups() {
  const { handleUnauthorized } = useAuth()
  const [groups, setGroups] = useState<Group[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    const result = await getUserGroups()
    if (result.status === 401) { handleUnauthorized(result.body); return }
    if (result.ok && result.body?.data?.groups) {
      setGroups(result.body.data.groups as Group[])
    } else {
      setError('Failed to load groups')
    }
    setLoading(false)
  }, [handleUnauthorized])

  useEffect(() => { load() }, [load])

  return { groups, loading, error, reload: load }
}
