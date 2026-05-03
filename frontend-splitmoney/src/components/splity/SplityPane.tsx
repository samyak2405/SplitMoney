import { useEffect, useRef, useState } from 'react'
import { fetchSplityHistory, sendSplityMessage, SplityMessage, AttachmentInfo } from '../../api/splity'
import { uploadDocument } from '../../api/documents'

const AI_MAX_BASE64_BYTES = 5 * 1024 * 1024 // 5 MB — Claude/OpenAI multimodal limit

/** Reads a File as base64 (data-URL prefix stripped). Returns null if the file is too large. */
function readFileAsBase64(file: File): Promise<string | null> {
  if (file.size > AI_MAX_BASE64_BYTES) return Promise.resolve(null)
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      resolve(result.split(',')[1] ?? null) // strip "data:image/jpeg;base64," prefix
    }
    reader.onerror = () => reject(new Error('FileReader error'))
    reader.readAsDataURL(file)
  })
}

const ALLOWED_MIME_TYPES = new Set([
  'image/jpeg',
  'image/png',
  'image/gif',
  'image/webp',
  'application/pdf',
])
const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB

interface Props {
  groupId: string | number
  members: Array<{ email: string }>
  onExpenseCreated?: () => void
}

function relativeTime(epochMs: number): string {
  const diff = Date.now() - epochMs
  if (diff < 60_000) return 'just now'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m ago`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h ago`
  return new Date(epochMs).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

export default function SplityPane({ groupId, onExpenseCreated }: Props) {
  const [messages, setMessages] = useState<SplityMessage[]>([])
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(false)
  const [historyLoading, setHistoryLoading] = useState(true)
  const [focused, setFocused] = useState(false)
  const [pendingFile, setPendingFile] = useState<File | null>(null)
  const [uploadingFile, setUploadingFile] = useState(false)
  const [fileError, setFileError] = useState<string | null>(null)

  const bottomRef = useRef<HTMLDivElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const initialScrollDoneRef = useRef(false)
  const prevMsgCountRef = useRef(0)

  useEffect(() => {
    setHistoryLoading(true)
    initialScrollDoneRef.current = false
    prevMsgCountRef.current = 0
    fetchSplityHistory(String(groupId)).then(result => {
      if (result.ok && result.body?.data) {
        setMessages(result.body.data)
      }
      setHistoryLoading(false)
    })
  }, [groupId])

  function isNearBottom(): boolean {
    const el = containerRef.current
    if (!el) return true
    return el.scrollHeight - el.scrollTop - el.clientHeight < 150
  }

  useEffect(() => {
    if (historyLoading) return
    if (!initialScrollDoneRef.current) {
      initialScrollDoneRef.current = true
      prevMsgCountRef.current = messages.length
      bottomRef.current?.scrollIntoView({ behavior: 'instant' as ScrollBehavior })
      return
    }
    if (messages.length > prevMsgCountRef.current) {
      prevMsgCountRef.current = messages.length
      if (isNearBottom()) {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
      }
    }
  }, [historyLoading, messages])

  const canSend = (text.trim().length > 0 || pendingFile !== null) && !loading && !uploadingFile

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    // Reset input so the same file can be re-selected if removed then re-added
    e.target.value = ''
    if (!file) return

    setFileError(null)

    if (!ALLOWED_MIME_TYPES.has(file.type)) {
      setFileError('Only images (JPEG, PNG, GIF, WEBP) and PDF files are supported.')
      return
    }
    if (file.size > MAX_FILE_SIZE_BYTES) {
      setFileError(`File is too large (${formatBytes(file.size)}). Maximum size is 10 MB.`)
      return
    }

    setPendingFile(file)
  }

  const handleSend = async () => {
    if (!canSend) return
    const content = text.trim()
    const file = pendingFile

    setText('')
    setPendingFile(null)
    setFileError(null)
    if (textareaRef.current) textareaRef.current.style.height = 'auto'

    // Optimistic: show user message immediately (with attachment label if applicable)
    const displayContent = file
      ? (content ? `[Bill: ${file.name}]\n${content}` : `[Bill: ${file.name}]`)
      : content
    const optimisticUser: SplityMessage = {
      messageId: `opt-${Date.now()}`,
      role: 'user',
      content: displayContent,
      createdAtEpochMs: Date.now(),
    }
    setMessages(prev => [...prev, optimisticUser])
    setLoading(true)

    let attachment: AttachmentInfo | undefined

    if (file) {
      setUploadingFile(true)
      try {
        // Run upload and base64 encoding in parallel — both use the in-memory file
        const [uploadResult, base64] = await Promise.all([
          uploadDocument(String(groupId), file),
          readFileAsBase64(file).catch(() => null),
        ])

        if (!uploadResult.ok || !uploadResult.body?.data) {
          const errMsg = uploadResult.body?.responseMessage ?? 'Failed to upload the file. Please try again.'
          setMessages(prev => [...prev, {
            messageId: `err-${Date.now()}`,
            role: 'assistant',
            content: errMsg,
            createdAtEpochMs: Date.now(),
          }])
          setLoading(false)
          setUploadingFile(false)
          return
        }

        const doc = uploadResult.body.data
        attachment = {
          documentId: doc.documentId,
          documentMimeType: doc.mimeType,
          documentName: doc.fileName,
          // base64 is null if file > 5 MB (AI will fall back to document-service fetch)
          documentBase64: base64 ?? undefined,
        }
      } catch {
        setMessages(prev => [...prev, {
          messageId: `err-${Date.now()}`,
          role: 'assistant',
          content: 'Failed to upload the file. Please check your connection and try again.',
          createdAtEpochMs: Date.now(),
        }])
        setLoading(false)
        setUploadingFile(false)
        return
      } finally {
        setUploadingFile(false)
      }
    }

    try {
      const result = await sendSplityMessage(String(groupId), content, attachment)
      if (result.ok && result.body?.data) {
        setMessages(prev => [...prev, result.body!.data!])
        if (result.body.data.expenseCreated) {
          onExpenseCreated?.()
        }
      } else {
        setMessages(prev => [...prev, {
          messageId: `err-${Date.now()}`,
          role: 'assistant',
          content: 'Sorry, I had trouble responding. Please try again.',
          createdAtEpochMs: Date.now(),
        }])
      }
    } catch {
      setMessages(prev => [...prev, {
        messageId: `err-${Date.now()}`,
        role: 'assistant',
        content: 'Connection error. Please check your network and try again.',
        createdAtEpochMs: Date.now(),
      }])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setText(e.target.value)
    const el = e.target
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 96) + 'px'
  }

  return (
    <div style={{
      border: '1px solid var(--border-subtle, #E5E7EB)',
      borderRadius: 16,
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column',
      height: '65vh',
      background: 'white',
    }}>
      {/* Header */}
      <div style={{
        padding: '14px 20px',
        borderBottom: '1px solid var(--border-subtle)',
        background: 'linear-gradient(135deg, #faf5ff, #ede9fe)',
        display: 'flex', alignItems: 'center', gap: 10, flexShrink: 0,
      }}>
        <div style={{
          width: 36, height: 36, borderRadius: '50%',
          background: 'linear-gradient(135deg, #7c3aed, #4f46e5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: 'white', fontWeight: 800, fontSize: 11,
          boxShadow: '0 2px 8px rgba(124,58,237,0.35)',
        }}>AI</div>
        <div>
          <div style={{ fontWeight: 700, fontSize: 15, color: '#4c1d95' }}>Splity</div>
          <div style={{ fontSize: 12, color: '#7c3aed' }}>Your AI expense assistant</div>
        </div>
      </div>

      {/* Messages */}
      <div ref={containerRef} style={{ flex: 1, overflowY: 'auto', padding: '16px 16px 8px' }}>
        {historyLoading ? (
          <div style={{ textAlign: 'center', padding: 40, color: 'var(--fg3)', fontSize: 13 }}>
            Loading conversation…
          </div>
        ) : messages.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 20px' }}>
            <div style={{ fontSize: 32, marginBottom: 12 }}>✨</div>
            <div style={{ fontWeight: 700, color: '#4c1d95', marginBottom: 6 }}>Hi! I'm Splity</div>
            <div style={{ fontSize: 13, color: 'var(--fg3)', lineHeight: 1.6 }}>
              I can split expenses, check balances, and parse bills from photos or PDFs.<br />
              e.g. <em>"Split ₹2000 dinner paid by me equally"</em> or attach a receipt 📎
            </div>
          </div>
        ) : (
          messages.map(msg => (
            msg.role === 'user' ? (
              <UserBubble key={msg.messageId} msg={msg} />
            ) : (
              <SplityBubble key={msg.messageId} msg={msg} />
            )
          ))
        )}

        {loading && <ThinkingIndicator uploading={uploadingFile} />}
        <div ref={bottomRef} />
      </div>

      {/* Input area */}
      <div style={{
        borderTop: '1px solid var(--border-subtle)',
        background: 'white',
        flexShrink: 0,
        padding: '8px 14px 12px',
      }}>
        {/* File error */}
        {fileError && (
          <div style={{
            fontSize: 12, color: '#dc2626', marginBottom: 6,
            padding: '4px 10px',
            background: '#fef2f2',
            borderRadius: 8,
            border: '1px solid #fecaca',
          }}>
            {fileError}
          </div>
        )}

        {/* Pending file chip */}
        {pendingFile && (
          <div style={{
            display: 'flex', alignItems: 'center', gap: 6,
            marginBottom: 6,
            padding: '5px 10px',
            background: '#f5f3ff',
            border: '1px solid #c4b5fd',
            borderRadius: 20,
            width: 'fit-content',
            maxWidth: '100%',
          }}>
            <span style={{ fontSize: 13 }}>📎</span>
            <span style={{
              fontSize: 12, color: '#5b21b6', fontWeight: 600,
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 200,
            }}>
              {pendingFile.name}
            </span>
            <span style={{ fontSize: 11, color: '#7c3aed', flexShrink: 0 }}>
              {formatBytes(pendingFile.size)}
            </span>
            <button
              onClick={() => { setPendingFile(null); setFileError(null) }}
              style={{
                background: 'none', border: 'none', cursor: 'pointer',
                color: '#7c3aed', fontSize: 14, padding: '0 2px', lineHeight: 1,
                flexShrink: 0,
              }}
              title="Remove attachment"
            >
              ×
            </button>
          </div>
        )}

        {/* Input row */}
        <div style={{
          display: 'flex', alignItems: 'flex-end', gap: 8,
        }}>
          {/* Hidden file input */}
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp,application/pdf"
            style={{ display: 'none' }}
            onChange={handleFileSelect}
          />

          <div style={{
            flex: 1,
            display: 'flex', alignItems: 'flex-end',
            border: `1.5px solid ${focused ? '#7c3aed' : 'var(--border-default, #E5E7EB)'}`,
            borderRadius: 24,
            background: 'white',
            transition: 'border-color 150ms',
            padding: '2px 4px 2px 6px',
            gap: 4,
          }}>
            {/* Attachment button */}
            <button
              onClick={() => { setFileError(null); fileInputRef.current?.click() }}
              disabled={loading || uploadingFile}
              title="Attach a bill or receipt"
              style={{
                width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
                border: 'none', cursor: loading || uploadingFile ? 'default' : 'pointer',
                background: pendingFile ? '#ede9fe' : 'transparent',
                color: pendingFile ? '#7c3aed' : 'var(--fg3, #9ca3af)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                transition: 'color 150ms, background 150ms',
                marginBottom: 2,
              }}
            >
              {uploadingFile ? (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"
                  style={{ animation: 'spin 1s linear infinite' }}>
                  <circle cx="12" cy="12" r="10" strokeOpacity="0.25" />
                  <path d="M12 2a10 10 0 0 1 10 10" />
                </svg>
              ) : (
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                  stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" />
                </svg>
              )}
            </button>

            <textarea
              ref={textareaRef}
              value={text}
              onChange={handleChange}
              onKeyDown={handleKeyDown}
              onFocus={() => setFocused(true)}
              onBlur={() => setFocused(false)}
              disabled={loading}
              placeholder={
                loading ? 'Splity is thinking…'
                : uploadingFile ? 'Uploading file…'
                : pendingFile ? 'Add a message or send the bill as-is…'
                : 'Split an expense, attach a bill, or ask about balances…'
              }
              rows={1}
              style={{
                flex: 1,
                resize: 'none',
                border: 'none',
                outline: 'none',
                padding: '8px 0',
                fontSize: 14,
                fontFamily: 'inherit',
                lineHeight: 1.45,
                background: 'transparent',
                color: 'var(--fg1)',
                overflowY: 'hidden',
              }}
            />
            <button
              onClick={handleSend}
              disabled={!canSend}
              style={{
                width: 34, height: 34, borderRadius: '50%', flexShrink: 0,
                border: 'none',
                cursor: canSend ? 'pointer' : 'default',
                background: canSend
                  ? 'linear-gradient(135deg, #7c3aed, #4f46e5)'
                  : 'var(--border-default, #E5E7EB)',
                color: 'white',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                transition: 'background 150ms, transform 100ms',
                transform: canSend ? 'scale(1)' : 'scale(0.92)',
                marginBottom: 2,
              }}
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      <style>{`
        @keyframes spin {
          from { transform: rotate(0deg); }
          to   { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  )
}

// ── Bubble helpers ────────────────────────────────────────────────────────────

/** Parses "[Bill: filename.jpg]\nrest of message" prefix from a stored message. */
function parseBillContent(content: string): { fileName: string | null; text: string } {
  const match = content.match(/^\[Bill: (.+?)\]\n?(.*)$/s)
  if (match) {
    return { fileName: match[1], text: match[2].trim() }
  }
  return { fileName: null, text: content }
}

function UserBubble({ msg }: { msg: SplityMessage }) {
  const { fileName, text } = parseBillContent(msg.content)

  return (
    <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '2px 0' }}>
      <div style={{ maxWidth: '68%', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 3 }}>
        {/* Attachment chip */}
        {fileName && (
          <div style={{
            display: 'flex', alignItems: 'center', gap: 5,
            background: 'linear-gradient(135deg, #16a34a22, #15803d22)',
            border: '1px solid #16a34a55',
            borderRadius: 12,
            padding: '3px 10px',
            fontSize: 11, color: '#15803d', fontWeight: 600,
          }}>
            <span>📎</span>
            <span style={{
              maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            }}>
              {fileName}
            </span>
          </div>
        )}

        {/* Message text (only if non-empty) */}
        {text && (
          <div style={{
            background: 'linear-gradient(135deg, #16a34a, #15803d)',
            color: 'white',
            padding: '9px 13px',
            borderRadius: '18px 18px 4px 18px',
            fontSize: 14, lineHeight: 1.5,
            wordBreak: 'break-word',
            boxShadow: '0 2px 8px rgba(22,163,74,0.25)',
            whiteSpace: 'pre-wrap',
          }}>
            {text}
          </div>
        )}

        <span style={{ fontSize: 10, color: 'var(--fg3)', paddingInline: 4 }}>
          {relativeTime(msg.createdAtEpochMs)}
        </span>
      </div>
    </div>
  )
}

function SplityBubble({ msg }: { msg: SplityMessage }) {
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, padding: '2px 0' }}>
      <div style={{
        width: 32, height: 32, borderRadius: '50%',
        background: 'linear-gradient(135deg, #7c3aed, #4f46e5)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: 'white', fontWeight: 800, fontSize: 10, flexShrink: 0,
        boxShadow: '0 1px 4px rgba(124,58,237,0.35)',
      }}>AI</div>
      <div style={{ maxWidth: '70%', display: 'flex', flexDirection: 'column', gap: 3 }}>
        <span style={{ fontSize: 11, fontWeight: 700, color: '#7c3aed', paddingLeft: 4 }}>Splity</span>
        <div style={{
          background: 'linear-gradient(135deg, #ede9fe, #e0e7ff)',
          color: '#3730a3',
          padding: '10px 14px',
          borderRadius: '18px 18px 18px 4px',
          fontSize: 14, lineHeight: 1.55,
          wordBreak: 'break-word',
          border: '1px solid #c4b5fd',
          boxShadow: '0 1px 4px rgba(124,58,237,0.12)',
          whiteSpace: 'pre-wrap',
        }}>
          {msg.content}
        </div>
        <span style={{ fontSize: 10, color: 'var(--fg3)', paddingInline: 4 }}>
          {relativeTime(msg.createdAtEpochMs)}
        </span>
      </div>
    </div>
  )
}

function ThinkingIndicator({ uploading }: { uploading: boolean }) {
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, padding: '6px 0' }}>
      <div style={{
        width: 32, height: 32, borderRadius: '50%',
        background: 'linear-gradient(135deg, #7c3aed, #4f46e5)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        color: 'white', fontWeight: 800, fontSize: 10, flexShrink: 0,
      }}>AI</div>
      <div style={{
        background: 'linear-gradient(135deg, #ede9fe, #e0e7ff)',
        border: '1px solid #c4b5fd',
        padding: '10px 16px',
        borderRadius: '18px 18px 18px 4px',
        display: 'flex', gap: 5, alignItems: 'center',
        fontSize: 12, color: '#7c3aed',
      }}>
        {uploading ? (
          <span style={{ fontStyle: 'italic' }}>Uploading bill…</span>
        ) : (
          <>
            {[0, 1, 2].map(i => (
              <span key={i} style={{
                width: 7, height: 7, borderRadius: '50%',
                background: '#7c3aed',
                display: 'inline-block',
                animation: `splityPulse 1.2s ease-in-out ${i * 0.2}s infinite`,
              }} />
            ))}
          </>
        )}
      </div>
      <style>{`
        @keyframes splityPulse {
          0%, 80%, 100% { opacity: 0.3; transform: scale(0.85); }
          40% { opacity: 1; transform: scale(1); }
        }
      `}</style>
    </div>
  )
}
