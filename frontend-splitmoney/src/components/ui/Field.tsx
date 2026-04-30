export default function Field({ label, children, hint }) {
  return (
    <label style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--fg1)' }}>{label}</span>
      {children}
      {hint && <span style={{ fontSize: 12, color: 'var(--fg3)' }}>{hint}</span>}
    </label>
  )
}
