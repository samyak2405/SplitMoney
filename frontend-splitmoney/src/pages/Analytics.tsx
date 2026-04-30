import AppShell from '../components/layout/AppShell'
import StatCard from '../components/ui/StatCard'
import Card from '../components/ui/Card'
import { CATEGORIES, fmt } from '../data'

const CAT_TOTALS = [
  { cat: 'food',          amount: 842.30 },
  { cat: 'travel',        amount: 524.00 },
  { cat: 'rent',          amount: 2800.00 },
  { cat: 'utilities',     amount: 207.18 },
  { cat: 'groceries',     amount: 251.72 },
  { cat: 'entertainment', amount: 180.00 },
  { cat: 'other',         amount: 175.68 },
]

const MONTHS = [
  { m: 'Nov', v: 2180 }, { m: 'Dec', v: 3410 }, { m: 'Jan', v: 2820 },
  { m: 'Feb', v: 1940 }, { m: 'Mar', v: 3780 }, { m: 'Apr', v: 4180 },
]
const MAX_MONTH = 4200

export default function Analytics() {
  const total = CAT_TOTALS.reduce((s, c) => s + c.amount, 0)
  const sorted = [...CAT_TOTALS].sort((a, b) => b.amount - a.amount)

  return (
    <AppShell title="Analytics" subtitle="Where your shared money goes.">
      {/* Stat row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
        <StatCard label="This month"   value="₹1,842"   sub="↑ 12% vs last month" />
        <StatCard label="Year to date" value="₹14,230" />
        <StatCard label="Avg / expense" value="₹48.21" />
        <StatCard label="Top category" value="Rent" accent="var(--sm-green-600)" sub="56.7% of spending" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: 24 }}>
        {/* Bar chart */}
        <Card style={{ padding: 28 }}>
          <h3 style={{ margin: '0 0 20px', fontSize: 18, fontWeight: 600 }}>Monthly spending</h3>
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: 14, height: 240 }}>
            {MONTHS.map((m, i) => (
              <div
                key={m.m}
                style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}
              >
                <div style={{ fontSize: 11, fontVariantNumeric: 'tabular-nums', color: 'var(--fg3)', fontWeight: 600 }}>
                  ₹{Math.round(m.v / 100) / 10}k
                </div>
                <div
                  style={{
                    width: '100%',
                    height: `${(m.v / MAX_MONTH) * 180}px`,
                    background: i === MONTHS.length - 1 ? 'var(--grad-brand)' : 'var(--sm-green-100)',
                    borderRadius: '10px 10px 4px 4px',
                    minHeight: 8,
                    transition: 'all 300ms',
                  }}
                />
                <div style={{ fontSize: 12, color: 'var(--fg3)', fontWeight: 600 }}>{m.m}</div>
              </div>
            ))}
          </div>
        </Card>

        {/* Category breakdown */}
        <Card style={{ padding: 28 }}>
          <h3 style={{ margin: '0 0 20px', fontSize: 18, fontWeight: 600 }}>By category</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {sorted.map(c => {
              const cat = CATEGORIES.find(x => x.key === c.cat)
              const pct = (c.amount / total) * 100
              return (
                <div key={c.cat}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 6 }}>
                    <span style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span>{cat.emoji}</span>{cat.label}
                    </span>
                    <span style={{ fontVariantNumeric: 'tabular-nums', color: 'var(--fg3)' }}>
                      {fmt(c.amount)} · {pct.toFixed(0)}%
                    </span>
                  </div>
                  <div style={{ height: 6, background: 'var(--bg3)', borderRadius: 999, overflow: 'hidden' }}>
                    <div
                      style={{ width: `${pct}%`, height: '100%', background: cat.dot, borderRadius: 999 }}
                    />
                  </div>
                </div>
              )
            })}
          </div>
        </Card>
      </div>
    </AppShell>
  )
}
