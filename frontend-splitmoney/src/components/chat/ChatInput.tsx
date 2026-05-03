import { useRef, useState } from 'react'
import { useChatContext } from '../../context/ChatContext'
import FileUploadButton from './FileUploadButton'

interface ChatInputProps {
  groupId: string
}

export default function ChatInput({ groupId }: ChatInputProps) {
  const { sendMessage, sendTyping, isConnected } = useChatContext()
  const [text, setText] = useState('')
  const [focused, setFocused] = useState(false)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  const canSend = text.trim().length > 0 && isConnected

  const handleSend = () => {
    if (!canSend) return
    sendMessage(text.trim())
    setText('')
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
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
    sendTyping()
    const el = e.target
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 96) + 'px'
  }

  return (
    <div style={{
      display: 'flex', alignItems: 'flex-end', gap: 8,
      padding: '10px 14px 12px',
      borderTop: '1px solid var(--border-subtle, #F3F4F6)',
      background: 'white',
    }}>
      <FileUploadButton groupId={groupId} />

      <div style={{
        flex: 1,
        display: 'flex', alignItems: 'flex-end',
        border: `1.5px solid ${focused ? 'var(--sm-green-600, #16a34a)' : 'var(--border-default, #E5E7EB)'}`,
        borderRadius: 24,
        background: isConnected ? 'white' : 'var(--border-subtle, #F9FAFB)',
        transition: 'border-color 150ms',
        padding: '2px 4px 2px 14px',
        gap: 6,
      }}>
        <textarea
          ref={textareaRef}
          value={text}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          disabled={!isConnected}
          placeholder={isConnected ? 'Write a message…' : 'Connecting…'}
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
          title="Send (Enter)"
          style={{
            width: 34, height: 34, borderRadius: '50%', flexShrink: 0,
            border: 'none',
            cursor: canSend ? 'pointer' : 'default',
            background: canSend
              ? 'linear-gradient(135deg, #16a34a, #15803d)'
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
  )
}
