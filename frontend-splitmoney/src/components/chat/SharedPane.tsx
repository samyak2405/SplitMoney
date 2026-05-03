import { useEffect, useState } from 'react'
import { listDocuments, type DocumentItem } from '../../api/documents'
import { useChatContext } from '../../context/ChatContext'

const URL_REGEX = /(https?:\/\/[^\s]+)/g

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function relativeTime(epochMs: number): string {
  const diff = Date.now() - epochMs
  if (diff < 60_000) return 'just now'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m ago`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h ago`
  return new Date(epochMs).toLocaleDateString()
}

interface LinkEntry {
  url: string
  senderEmail: string
  epochMs: number
}

interface Props {
  groupId: string
}

export default function SharedPane({ groupId }: Props) {
  const { messages } = useChatContext()
  const [documents, setDocuments] = useState<DocumentItem[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    listDocuments(groupId).then(result => {
      if (result.ok && result.body?.data) {
        setDocuments(result.body.data)
      }
      setLoading(false)
    })
  }, [groupId])

  // Extract unique links from text messages
  const links: LinkEntry[] = []
  const seen = new Set<string>()
  for (const msg of messages) {
    if (msg.msgType !== 'TEXT') continue
    const found = msg.content.match(URL_REGEX)
    if (!found) continue
    for (const url of found) {
      const clean = url.replace(/[.)]+$/, '') // strip trailing punctuation
      if (seen.has(clean)) continue
      seen.add(clean)
      links.push({ url: clean, senderEmail: msg.senderEmail, epochMs: msg.createdAtEpochMs })
    }
  }

  const sectionHeader = (title: string) => (
    <div style={{
      fontSize: 11, fontWeight: 700, color: 'var(--fg3)',
      textTransform: 'uppercase', letterSpacing: '0.06em',
      padding: '12px 16px 6px',
    }}>
      {title}
    </div>
  )

  return (
    <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
      {sectionHeader('Files')}

      {loading && (
        <div style={{ padding: '8px 16px', color: 'var(--fg3)', fontSize: 13 }}>Loading…</div>
      )}

      {!loading && documents.length === 0 && (
        <div style={{ padding: '8px 16px', color: 'var(--fg3)', fontSize: 13 }}>No files shared yet.</div>
      )}

      {documents.map(doc => {
        const isImage = doc.mimeType.startsWith('image/')
        return (
          <div key={doc.documentId} style={{
            display: 'flex', alignItems: 'center', gap: 12,
            padding: '10px 16px',
            borderBottom: '1px solid var(--border-subtle, #F3F4F6)',
          }}>
            <span style={{ fontSize: 22, flexShrink: 0 }}>{isImage ? '🖼️' : '📄'}</span>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{
                fontSize: 13, fontWeight: 600,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
              }}>
                {doc.fileName}
              </div>
              <div style={{ fontSize: 11, color: 'var(--fg3)' }}>
                {formatBytes(doc.fileSize)} · {doc.uploaderEmail.split('@')[0]} · {relativeTime(doc.createdAt)}
              </div>
            </div>
            <a
              href={doc.url}
              download={doc.fileName}
              style={{
                fontSize: 13, fontWeight: 600,
                color: 'var(--sm-green-600, #12B35E)',
                textDecoration: 'none', flexShrink: 0,
              }}
              title="Download"
            >
              ↓ Download
            </a>
          </div>
        )
      })}

      {sectionHeader('Links')}

      {links.length === 0 && (
        <div style={{ padding: '8px 16px', color: 'var(--fg3)', fontSize: 13 }}>No shared links yet.</div>
      )}

      {links.map(link => {
        let displayUrl: string
        try {
          const parsed = new URL(link.url)
          displayUrl = parsed.hostname + (parsed.pathname !== '/' ? parsed.pathname : '')
        } catch {
          displayUrl = link.url
        }
        return (
          <div key={link.url} style={{
            display: 'flex', alignItems: 'center', gap: 12,
            padding: '10px 16px',
            borderBottom: '1px solid var(--border-subtle, #F3F4F6)',
          }}>
            <span style={{ fontSize: 18, flexShrink: 0 }}>🔗</span>
            <div style={{ flex: 1, minWidth: 0 }}>
              <a
                href={link.url}
                target="_blank"
                rel="noopener noreferrer"
                style={{
                  fontSize: 13, fontWeight: 600,
                  color: 'var(--sm-green-600, #12B35E)',
                  textDecoration: 'none',
                  overflow: 'hidden', textOverflow: 'ellipsis',
                  display: 'block', whiteSpace: 'nowrap',
                }}
              >
                {displayUrl}
              </a>
              <div style={{ fontSize: 11, color: 'var(--fg3)' }}>
                {link.senderEmail.split('@')[0]} · {relativeTime(link.epochMs)}
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}
