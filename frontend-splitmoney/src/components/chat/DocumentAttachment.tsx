import { useState } from 'react'
import type { DocumentItem } from '../../api/documents'

interface Props {
  doc: DocumentItem
  isMine: boolean
  senderEmail: string
  createdAtEpochMs: number
  showSender?: boolean
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

function FileIcon({ mimeType, color }: { mimeType: string; color: string }) {
  const isPdf = mimeType === 'application/pdf'
  return (
    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke={color}
      strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" style={{ flexShrink: 0 }}>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      {isPdf && <text x="6" y="18" fontSize="5" fontWeight="bold" stroke="none" fill={color}>PDF</text>}
    </svg>
  )
}

export default function DocumentAttachment({ doc, isMine, senderEmail, createdAtEpochMs, showSender }: Props) {
  const [hovered, setHovered] = useState(false)
  const isImage = doc.mimeType.startsWith('image/')
  const initial = senderEmail.charAt(0).toUpperCase()

  const iconColor = isMine ? 'rgba(255,255,255,0.9)' : 'var(--sm-green-600, #16a34a)'

  const bubble = isImage ? (
    <a
      href={doc.url}
      target="_blank"
      rel="noopener noreferrer"
      style={{
        display: 'block',
        borderRadius: 14,
        overflow: 'hidden',
        maxWidth: 260,
        boxShadow: '0 2px 8px rgba(0,0,0,0.12)',
      }}
    >
      <img
        src={doc.url}
        alt={doc.fileName}
        style={{ display: 'block', maxWidth: 260, maxHeight: 200, objectFit: 'cover', width: '100%' }}
      />
      <div style={{
        padding: '6px 10px', fontSize: 11,
        background: isMine ? 'rgba(0,0,0,0.35)' : 'rgba(0,0,0,0.06)',
        color: isMine ? 'white' : 'var(--fg2)',
      }}>
        {doc.fileName}
      </div>
    </a>
  ) : (
    <a
      href={doc.url}
      download={doc.fileName}
      style={{ textDecoration: 'none' }}
    >
      <div
        onMouseEnter={() => setHovered(true)}
        onMouseLeave={() => setHovered(false)}
        style={{
          display: 'flex', alignItems: 'center', gap: 10,
          padding: '10px 14px',
          borderRadius: 14,
          background: isMine
            ? 'linear-gradient(135deg, #16a34a, #15803d)'
            : 'var(--border-subtle, #F3F4F6)',
          boxShadow: isMine
            ? '0 2px 8px rgba(22,163,74,0.25)'
            : '0 1px 3px rgba(0,0,0,0.07)',
          maxWidth: 260,
          cursor: 'pointer',
          transition: 'opacity 150ms',
          opacity: hovered ? 0.9 : 1,
        }}
      >
        <FileIcon mimeType={doc.mimeType} color={iconColor} />
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontWeight: 600, fontSize: 13,
            color: isMine ? 'white' : 'var(--fg1)',
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          }}>
            {doc.fileName}
          </div>
          <div style={{ fontSize: 11, color: isMine ? 'rgba(255,255,255,0.7)' : 'var(--fg3)', marginTop: 2 }}>
            {formatBytes(doc.fileSize)}
          </div>
        </div>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none"
          stroke={iconColor} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
          style={{ flexShrink: 0, opacity: 0.8 }}>
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
          <polyline points="7 10 12 15 17 10" />
          <line x1="12" y1="15" x2="12" y2="3" />
        </svg>
      </div>
    </a>
  )

  return (
    <div style={{
      display: 'flex',
      flexDirection: isMine ? 'row-reverse' : 'row',
      alignItems: 'flex-end',
      gap: 8,
    }}>
      {!isMine && (
        <div style={{
          width: 32, height: 32, borderRadius: '50%',
          background: 'linear-gradient(135deg, #16a34a, #15803d)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#fff', fontSize: 13, fontWeight: 700, flexShrink: 0,
          boxShadow: '0 1px 3px rgba(0,0,0,0.15)',
          visibility: showSender !== false ? 'visible' : 'hidden',
        }}>
          {initial}
        </div>
      )}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: isMine ? 'flex-end' : 'flex-start', gap: 3 }}>
        {showSender && !isMine && (
          <span style={{
            fontSize: 11, fontWeight: 600,
            color: 'var(--sm-green-600, #16a34a)',
            paddingLeft: 4,
          }}>
            {senderEmail.split('@')[0]}
          </span>
        )}
        {bubble}
        <span style={{ fontSize: 10, color: 'var(--fg3)', paddingInline: 4 }}>
          {relativeTime(createdAtEpochMs)}
        </span>
      </div>
    </div>
  )
}
