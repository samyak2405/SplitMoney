const CURRENCY_SYMBOLS: Record<string, string> = {
  INR: '₹', EUR: '€', GBP: '£', USD: '$',
}

export function currencySymbol(currency: string): string {
  return CURRENCY_SYMBOLS[currency] ?? currency
}

export function fmt(n: number | string, currency = 'INR'): string {
  const sym = currencySymbol(currency)
  const abs = Math.abs(Number(n) || 0).toFixed(2)
  const [w, f] = abs.split('.')
  return `${sym}${Number(w).toLocaleString()}.${f}`
}
