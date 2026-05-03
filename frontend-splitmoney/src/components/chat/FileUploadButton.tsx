import { useRef, useState } from 'react'
import { uploadDocument } from '../../api/documents'
import { useChatContext } from '../../context/ChatContext'

const MAX_MB = 10
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'application/pdf']

interface Props {
  groupId: string
}

export default function FileUploadButton({ groupId }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { sendMessage, isConnected } = useChatContext()

  const handleClick = () => {
    if (!isConnected || loading) return
    inputRef.current?.click()
  }

  const handleChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!inputRef.current) return
    inputRef.current.value = ''
    if (!file) return

    setError(null)

    if (!ALLOWED_TYPES.includes(file.type)) {
      setError('Only JPEG, PNG, GIF, WEBP and PDF files are allowed')
      return
    }

    if (file.size > MAX_MB * 1024 * 1024) {
      setError(`File must be under ${MAX_MB} MB`)
      return
    }

    setLoading(true)
    try {
      const result = await uploadDocument(groupId, file)
      if (!result.ok || !result.body?.data) {
        setError(result.body?.responseMessage ?? 'Upload failed')
        return
      }
      const doc = result.body.data
      const payload = JSON.stringify({
        documentId: doc.documentId,
        fileName: doc.fileName,
        fileSize: doc.fileSize,
        mimeType: doc.mimeType,
        url: doc.url,
        uploaderEmail: doc.uploaderEmail,
        createdAt: doc.createdAt,
      })
      sendMessage(payload, 'DOCUMENT')
    } catch {
      setError('Upload failed — please try again')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ position: 'relative', flexShrink: 0 }}>
      <input
        ref={inputRef}
        type="file"
        accept=".jpg,.jpeg,.png,.gif,.webp,.pdf"
        style={{ display: 'none' }}
        onChange={handleChange}
      />
      <button
        onClick={handleClick}
        disabled={!isConnected || loading}
        title="Attach file"
        style={{
          width: 38, height: 38,
          border: 'none',
          borderRadius: 8,
          background: 'var(--border-subtle, #F3F4F6)',
          cursor: isConnected && !loading ? 'pointer' : 'not-allowed',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 18, flexShrink: 0,
          opacity: isConnected ? 1 : 0.5,
          transition: 'background 150ms',
        }}
      >
        {loading ? '⏳' : '📎'}
      </button>
      {error && (
        <div style={{
          position: 'absolute', bottom: '110%', left: 0,
          background: '#EF4444', color: '#fff',
          padding: '4px 8px', borderRadius: 6,
          fontSize: 11, whiteSpace: 'nowrap', zIndex: 10,
          maxWidth: 220,
        }}>
          {error}
        </div>
      )}
    </div>
  )
}
