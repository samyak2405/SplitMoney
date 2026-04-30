import { forwardRef } from 'react'

const Input = forwardRef(function Input({ style = {}, ...rest }, ref) {
  return (
    <input
      ref={ref}
      {...rest}
      style={{
        fontFamily: 'var(--font-sans)',
        fontSize: 15,
        padding: '12px 14px',
        borderRadius: 12,
        border: '1.5px solid var(--border-default)',
        background: 'white',
        color: 'var(--fg1)',
        outline: 'none',
        transition: 'all 180ms',
        width: '100%',
        ...style,
      }}
    />
  )
})

export default Input
