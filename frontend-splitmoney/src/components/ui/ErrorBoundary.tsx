import { Component, type ErrorInfo, type ReactNode } from 'react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback
      return (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', minHeight: '100vh', gap: 16, padding: 32,
          fontFamily: 'var(--font-sans)',
        }}>
          <div style={{ fontSize: 40 }}>⚠️</div>
          <h2 style={{ margin: 0, fontSize: 22, fontWeight: 700, color: 'var(--fg1)' }}>
            Something went wrong
          </h2>
          <p style={{ margin: 0, color: 'var(--fg3)', fontSize: 14, maxWidth: 400, textAlign: 'center' }}>
            {this.state.error?.message ?? 'An unexpected error occurred.'}
          </p>
          <button
            onClick={() => this.setState({ hasError: false, error: null })}
            style={{
              padding: '10px 20px', borderRadius: 10, border: 'none',
              background: 'var(--sm-green-500)', color: 'white',
              fontWeight: 600, cursor: 'pointer', fontSize: 14,
            }}
          >
            Try again
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
