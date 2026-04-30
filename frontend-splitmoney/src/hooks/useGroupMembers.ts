import { useCallback, useEffect, useState } from 'react'
import { getGroupDetails } from '../api/splitwise'
import { useAuth } from '../context/AuthContext'
import type { GroupMember } from '../types'

export function useGroupMembers(groupName: string | null) {
  const { handleUnauthorized } = useAuth()
  const [members, setMembers] = useState<GroupMember[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    if (!groupName) return
    setLoading(true)
    setError(null)
    const result = await getGroupDetails({ groupName })
    if (result.status === 401) { handleUnauthorized(result.body); return }
    if (result.ok && result.body?.data?.members) {
      setMembers(result.body.data.members as GroupMember[])
    } else {
      setError('Failed to load members')
    }
    setLoading(false)
  }, [groupName, handleUnauthorized])

  useEffect(() => { load() }, [load])

  return { members, loading, error, reload: load }
}
