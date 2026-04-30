import Sidebar from './Sidebar'
import TopBar from './TopBar'

export default function AppShell({ children, title, subtitle, topRight, back }) {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: '248px 1fr',
        minHeight: '100vh',
        background: 'var(--sm-paper)',
      }}
    >
      <Sidebar />
      <main style={{ minWidth: 0, overflow: 'hidden' }}>
        <TopBar title={title} subtitle={subtitle} right={topRight} back={back} />
        <div style={{ padding: '32px 40px 80px' }}>{children}</div>
      </main>
    </div>
  )
}
