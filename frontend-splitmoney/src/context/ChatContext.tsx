import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react'
import { Client } from '@stomp/stompjs'
import {
  fetchMessages,
  fetchPresence,
  fetchUnreadCursor,
  markGroupRead,
  MessageItem,
} from '../api/chat'
import { useAuth } from './AuthContext'

const WS_URL = (() => {
  const explicit = import.meta.env.VITE_CHAT_WS_URL as string | undefined
  if (explicit) return explicit
  const proto = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${proto}://${window.location.host}/ws-chat`
})()

const HEARTBEAT_MS = 20_000
const TYPING_DEBOUNCE_MS = 2_000

interface ChatContextValue {
  messages: MessageItem[]
  onlineUserIds: Set<string>
  typingUserEmails: Set<string>
  isConnected: boolean
  nextCursor: string | null
  loadingHistory: boolean
  lastSeenEpochMs: number | null
  sendMessage: (content: string, msgType?: string) => void
  sendTyping: () => void
  loadMore: () => void
}

const ChatContext = createContext<ChatContextValue | null>(null)

export function useChatContext() {
  const ctx = useContext(ChatContext)
  if (!ctx) throw new Error('useChatContext must be used inside ChatProvider')
  return ctx
}

export function ChatProvider({
  groupId,
  children,
}: {
  groupId: string
  children: React.ReactNode
}) {
  const { session } = useAuth()
  const [messages, setMessages] = useState<MessageItem[]>([])
  const [onlineUserIds, setOnlineUserIds] = useState<Set<string>>(new Set())
  const [typingUserEmails, setTypingUserEmails] = useState<Set<string>>(new Set())
  const [isConnected, setIsConnected] = useState(false)
  const [nextCursor, setNextCursor] = useState<string | null>(null)
  const [loadingHistory, setLoadingHistory] = useState(true)
  const [lastSeenEpochMs, setLastSeenEpochMs] = useState<number | null>(null)

  const clientRef = useRef<Client | null>(null)
  const heartbeatRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const lastTypingRef = useRef<number>(0)

  // Fetch unread cursor (last time user was in this chat)
  useEffect(() => {
    fetchUnreadCursor(groupId).then(result => {
      if (result.ok && result.body?.data) {
        setLastSeenEpochMs(result.body.data.lastSeenEpochMs ?? null)
      }
    })
  }, [groupId])

  // Load initial message history — Cassandra returns DESC (newest first), reverse to oldest-first
  useEffect(() => {
    setLoadingHistory(true)
    fetchMessages(groupId).then(result => {
      if (result.ok && result.body?.data) {
        setMessages(result.body.data.messages.slice().reverse())
        setNextCursor(result.body.data.nextCursor ?? null)
      }
      setLoadingHistory(false)
    })
  }, [groupId])

  // Load initial presence
  useEffect(() => {
    fetchPresence(groupId).then(result => {
      if (result.ok && result.body?.data) {
        setOnlineUserIds(new Set(result.body.data.onlineUserIds))
      }
    })
  }, [groupId])

  // Mark read when the user leaves this chat tab (component unmounts)
  useEffect(() => {
    return () => {
      markGroupRead(groupId)
    }
  }, [groupId])

  // WebSocket connection
  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
      reconnectDelay: 5000,
      onConnect: () => {
        setIsConnected(true)

        // New messages — append to end (oldest-first order maintained)
        client.subscribe(`/topic/chat/${groupId}`, msg => {
          const message: MessageItem = JSON.parse(msg.body)
          setMessages(prev => {
            if (prev.some(m => m.messageId === message.messageId)) return prev
            return [...prev, message]
          })
        })

        // Presence updates
        client.subscribe(`/topic/chat/${groupId}/presence`, msg => {
          const event = JSON.parse(msg.body)
          if (event.onlineUserIds) {
            setOnlineUserIds(new Set(event.onlineUserIds))
          }
        })

        // Typing indicators
        client.subscribe(`/topic/chat/${groupId}/typing`, msg => {
          const event = JSON.parse(msg.body)
          if (!event.email) return
          setTypingUserEmails(prev => {
            const next = new Set(prev)
            if (event.typing) {
              next.add(event.email)
              setTimeout(() => {
                setTypingUserEmails(cur => {
                  const s = new Set(cur)
                  s.delete(event.email)
                  return s
                })
              }, 4000)
            } else {
              next.delete(event.email)
            }
            return next
          })
        })

        // Heartbeat for presence
        heartbeatRef.current = setInterval(() => {
          if (client.connected) {
            client.publish({ destination: `/app/chat/${groupId}/heartbeat`, body: '' })
          }
        }, HEARTBEAT_MS)
        client.publish({ destination: `/app/chat/${groupId}/heartbeat`, body: '' })
      },
      onDisconnect: () => setIsConnected(false),
    })

    client.activate()
    clientRef.current = client

    return () => {
      if (heartbeatRef.current) clearInterval(heartbeatRef.current)
      client.deactivate()
    }
  }, [groupId])

  const sendMessage = useCallback((content: string, msgType = 'TEXT') => {
    const client = clientRef.current
    if (!client?.connected || !content.trim()) return
    client.publish({
      destination: `/app/chat/${groupId}/send`,
      body: JSON.stringify({ content: content.trim(), msgType }),
    })
  }, [groupId])

  const sendTyping = useCallback(() => {
    const client = clientRef.current
    if (!client?.connected) return
    const now = Date.now()
    if (now - lastTypingRef.current < TYPING_DEBOUNCE_MS) return
    lastTypingRef.current = now
    client.publish({ destination: `/app/chat/${groupId}/typing`, body: '' })
  }, [groupId])

  const loadMore = useCallback(() => {
    if (!nextCursor || loadingHistory) return
    setLoadingHistory(true)
    fetchMessages(groupId, nextCursor).then(result => {
      if (result.ok && result.body?.data) {
        // Older messages: reverse to oldest-first, prepend to current list
        setMessages(prev => [...result.body!.data!.messages.slice().reverse(), ...prev])
        setNextCursor(result.body.data.nextCursor ?? null)
      }
      setLoadingHistory(false)
    })
  }, [groupId, nextCursor, loadingHistory])

  return (
    <ChatContext.Provider
      value={{
        messages,
        onlineUserIds,
        typingUserEmails,
        isConnected,
        nextCursor,
        loadingHistory,
        lastSeenEpochMs,
        sendMessage,
        sendTyping,
        loadMore,
      }}
    >
      {children}
    </ChatContext.Provider>
  )
}
