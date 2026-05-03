import { useState } from 'react'
import { MessageItem } from '../../api/chat'
import DocumentAttachment from './DocumentAttachment'
import type { DocumentItem } from '../../api/documents'

interface MessageBubbleProps {
  message: MessageItem
  isMine: boolean
  showSender: boolean
}

function relativeTime(epochMs: number): string {
  const diff = Date.now() - epochMs
  if (diff < 60_000) return 'just now'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)}m ago`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)}h ago`
  return new Date(epochMs).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })
}

function avatarInitial(email: string): string {
  return (email.split('@')[0]?.[0] ?? '?').toUpperCase()
}

function parseDocumentContent(content: string): DocumentItem | null {
  try {
    return JSON.parse(content) as DocumentItem
  } catch {
    return null
  }
}

export default function MessageBubble({ message, isMine, showSender }: MessageBubbleProps) {
  const [hovered, setHovered] = useState(false)

  if (message.msgType === 'DOCUMENT') {
    const doc = message.documentMeta ?? parseDocumentContent(message.content)
    if (doc) {
      return (
        <div style={{ padding: '2px 0' }}>
          <DocumentAttachment
            doc={doc}
            isMine={isMine}
            senderEmail={message.senderEmail}
            createdAtEpochMs={message.createdAtEpochMs}
            showSender={showSender}
          />
        </div>
      )
    }
  }

  if (message.msgType === 'AI') {
    return (
      <div style={{
        display: 'flex', flexDirection: 'row', alignItems: 'flex-end',
        gap: 8, padding: '1px 0',
      }}>
        <div style={{
          width: 32, height: 32, borderRadius: '50%',
          background: 'linear-gradient(135deg, #7c3aed, #4f46e5)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: 'white', fontWeight: 800, fontSize: 10, flexShrink: 0,
          boxShadow: '0 1px 4px rgba(124,58,237,0.35)',
          letterSpacing: '-0.5px',
        }}>AI</div>
        <div style={{ maxWidth: '70%', display: 'flex', flexDirection: 'column', gap: 3 }}>
          <span style={{ fontSize: 11, fontWeight: 700, color: '#7c3aed', paddingLeft: 4 }}>
            Splity
          </span>
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
            {message.content}
          </div>
          <span style={{ fontSize: 10, color: 'var(--fg3)', paddingInline: 4 }}>
            {relativeTime(message.createdAtEpochMs)}
          </span>
        </div>
      </div>
    )
  }

  if (message.msgType === 'SYSTEM') {
    return (
      <div style={{ textAlign: 'center', padding: '4px 0' }}>
        <span style={{
          fontSize: 12, color: 'var(--fg3)', fontStyle: 'italic',
          background: 'var(--border-subtle, #F3F4F6)', padding: '3px 12px',
          borderRadius: 100,
        }}>
          {message.content}
        </span>
      </div>
    )
  }

  return (
    <div
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        display: 'flex',
        flexDirection: isMine ? 'row-reverse' : 'row',
        alignItems: 'flex-end',
        gap: 8,
        padding: '1px 0',
      }}
    >
      {/* Avatar */}
      {!isMine && (
        <div style={{
          width: 32, height: 32, borderRadius: '50%',
          background: 'linear-gradient(135deg, #16a34a, #15803d)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: 'white', fontWeight: 700, fontSize: 13,
          flexShrink: 0,
          boxShadow: '0 1px 3px rgba(0,0,0,0.15)',
          visibility: showSender ? 'visible' : 'hidden',
        }}>
          {avatarInitial(message.senderEmail)}
        </div>
      )}

      <div style={{
        maxWidth: '68%', display: 'flex', flexDirection: 'column', gap: 3,
        alignItems: isMine ? 'flex-end' : 'flex-start',
      }}>
        {showSender && !isMine && (
          <span style={{
            fontSize: 11, fontWeight: 600,
            color: 'var(--sm-green-600, #16a34a)',
            paddingLeft: 4, letterSpacing: '0.01em',
          }}>
            {message.senderEmail.split('@')[0]}
          </span>
        )}

        <div style={{
          background: isMine
            ? 'linear-gradient(135deg, #16a34a, #15803d)'
            : 'var(--border-subtle, #F3F4F6)',
          color: isMine ? 'white' : 'var(--fg1)',
          padding: '9px 13px',
          borderRadius: isMine ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
          fontSize: 14,
          lineHeight: 1.5,
          wordBreak: 'break-word',
          boxShadow: isMine
            ? '0 2px 8px rgba(22,163,74,0.25)'
            : '0 1px 3px rgba(0,0,0,0.07)',
          transition: 'box-shadow 150ms',
        }}>
          {message.content}
        </div>

        <span style={{
          fontSize: 10, color: 'var(--fg3)',
          paddingInline: 4,
          opacity: hovered ? 1 : 0.6,
          transition: 'opacity 150ms',
        }}>
          {relativeTime(message.createdAtEpochMs)}
        </span>
      </div>
    </div>
  )
}
