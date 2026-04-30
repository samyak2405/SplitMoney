import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Icon from '../components/ui/Icon'

function NavBar() {
  const navigate = useNavigate()
  return (
    <nav
      style={{
        position: 'sticky', top: 0, zIndex: 50,
        background: 'rgba(251,251,248,0.85)', backdropFilter: 'blur(12px)',
        borderBottom: '1px solid var(--border-subtle)',
        padding: '0 40px', height: 64,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ width: 32, height: 32, borderRadius: 10, background: 'var(--grad-brand)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: 18, color: '#03241A' }}>₹</div>
        <span style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 20, color: 'var(--sm-ink-950)', letterSpacing: '-0.02em' }}>SplitMoney</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <button
          onClick={() => navigate('/signin')}
          style={{ padding: '9px 18px', borderRadius: 999, border: '1.5px solid var(--border-strong)', background: 'transparent', cursor: 'pointer', fontWeight: 600, fontSize: 14, color: 'var(--fg1)' }}
        >
          Sign in
        </button>
        <button
          onClick={() => navigate('/signup')}
          style={{ padding: '9px 20px', borderRadius: 999, border: 'none', background: 'var(--grad-brand)', cursor: 'pointer', fontWeight: 600, fontSize: 14, color: '#03241A', boxShadow: '0 4px 12px rgba(18,179,94,.32)' }}
        >
          Get started free →
        </button>
      </div>
    </nav>
  )
}

function Hero() {
  const navigate = useNavigate()
  return (
    <section
      style={{
        padding: '100px 40px 80px',
        textAlign: 'center',
        position: 'relative',
        overflow: 'hidden',
        background: 'var(--sm-paper)',
      }}
    >
      {/* Background glow */}
      <div style={{ position: 'absolute', top: 0, left: '50%', transform: 'translateX(-50%)', width: 800, height: 600, background: 'radial-gradient(ellipse at 50% 0%, rgba(168,232,20,0.18) 0%, rgba(18,179,94,0.12) 40%, transparent 70%)', pointerEvents: 'none' }} />

      <div style={{ position: 'relative', maxWidth: 860, margin: '0 auto' }}>
        <div
          style={{
            display: 'inline-flex', alignItems: 'center', gap: 8, padding: '8px 16px',
            borderRadius: 999, background: 'var(--sm-green-50)', border: '1px solid var(--sm-green-200)',
            fontSize: 13, fontWeight: 600, color: 'var(--sm-green-700)', marginBottom: 32,
          }}
        >
          ✨ 200k+ groups trust SplitMoney
        </div>

        <h1
          style={{
            fontFamily: 'var(--font-display)',
            fontWeight: 800,
            fontSize: 'clamp(48px, 7vw, 88px)',
            letterSpacing: '-0.04em',
            lineHeight: 1.0,
            margin: 0,
            color: 'var(--sm-ink-950)',
          }}
        >
          Split expenses.
          <br />
          <span style={{ background: 'var(--grad-brand)', WebkitBackgroundClip: 'text', backgroundClip: 'text', color: 'transparent' }}>
            Not friendships.
          </span>
        </h1>

        <p
          style={{
            fontSize: 20, color: 'var(--fg2)', marginTop: 28, lineHeight: 1.6, maxWidth: 560, margin: '28px auto 0',
          }}
        >
          Add expenses, split any way, and settle up in seconds.
          The most delightful way to share costs with the people you love.
        </p>

        <div style={{ display: 'flex', gap: 12, justifyContent: 'center', marginTop: 40 }}>
          <button
            onClick={() => navigate('/signup')}
            style={{
              padding: '16px 32px', borderRadius: 999, border: 'none',
              background: 'var(--grad-brand)', cursor: 'pointer', fontWeight: 700, fontSize: 16,
              color: '#03241A', boxShadow: '0 8px 24px rgba(18,179,94,.36)',
            }}
          >
            Start for free →
          </button>
          <button
            onClick={() => navigate('/dashboard')}
            style={{
              padding: '16px 32px', borderRadius: 999, border: '1.5px solid var(--border-strong)',
              background: 'white', cursor: 'pointer', fontWeight: 600, fontSize: 16, color: 'var(--fg1)',
            }}
          >
            See a demo
          </button>
        </div>

        <p style={{ fontSize: 12, color: 'var(--fg3)', marginTop: 16 }}>
          Free forever · No credit card required
        </p>
      </div>
    </section>
  )
}

const FEATURES = [
  { emoji: '⚡', title: 'Add in seconds', desc: 'Snap a receipt or type an amount. SplitMoney calculates everyone\'s share instantly.' },
  { emoji: '🧮', title: 'Split any way', desc: 'Equal, by percentage, by item, or fully custom. Every group has its own logic.' },
  { emoji: '💸', title: 'Settle up fast', desc: 'Pay via Venmo, Cash App, PayPal, or mark settled in cash — one tap.' },
  { emoji: '📊', title: 'Track everything', desc: 'See where shared money goes with category breakdowns and monthly trends.' },
  { emoji: '🔔', title: 'Smart reminders', desc: 'Gentle nudges to remind friends. No awkward texts needed.' },
  { emoji: '🌍', title: 'Multi-currency', desc: 'Travel with friends internationally. We handle the conversion.' },
]

function Features() {
  return (
    <section style={{ padding: '80px 40px', background: 'white', borderTop: '1px solid var(--border-subtle)' }}>
      <div style={{ maxWidth: 1100, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: 60 }}>
          <h2 style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 'clamp(32px, 4vw, 52px)', letterSpacing: '-0.03em', margin: 0 }}>
            Everything you need.
          </h2>
          <p style={{ color: 'var(--fg2)', fontSize: 18, marginTop: 16 }}>
            We thought of the messy edge cases so you don't have to.
          </p>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 32 }}>
          {FEATURES.map(f => (
            <div key={f.title}>
              <div style={{ fontSize: 36, marginBottom: 16 }}>{f.emoji}</div>
              <h3 style={{ margin: '0 0 8px', fontSize: 20, fontWeight: 700, letterSpacing: '-0.01em' }}>{f.title}</h3>
              <p style={{ margin: 0, fontSize: 15, color: 'var(--fg2)', lineHeight: 1.6 }}>{f.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

function HowItWorks() {
  const steps = [
    { n: '01', title: 'Create a group', desc: 'Name it, pick an emoji, invite your people.' },
    { n: '02', title: 'Add expenses',   desc: 'Whoever paid logs it. Split equally or any way you like.' },
    { n: '03', title: 'Settle up',      desc: 'When it\'s time, pay in one tap. Everyone goes to zero.' },
  ]
  return (
    <section style={{ padding: '80px 40px', background: 'var(--sm-paper)' }}>
      <div style={{ maxWidth: 900, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: 60 }}>
          <h2 style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 'clamp(32px, 4vw, 52px)', letterSpacing: '-0.03em', margin: 0 }}>
            How it works.
          </h2>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 40 }}>
          {steps.map(s => (
            <div key={s.n} style={{ textAlign: 'center' }}>
              <div
                style={{
                  fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 64,
                  color: 'var(--sm-green-100)', letterSpacing: '-0.04em', lineHeight: 1, marginBottom: 16,
                }}
              >
                {s.n}
              </div>
              <h3 style={{ margin: '0 0 8px', fontSize: 20, fontWeight: 700 }}>{s.title}</h3>
              <p style={{ margin: 0, fontSize: 15, color: 'var(--fg2)', lineHeight: 1.6 }}>{s.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

const TESTIMONIALS = [
  { quote: 'Used to dread the post-trip spreadsheet. Not anymore.', name: 'Priya S.', role: 'Frequent traveler' },
  { quote: 'We use it for our apartment. The "settle up" flow is genius.', name: 'Jordan L.', role: 'Roommates user' },
  { quote: 'Finally an app that doesn\'t make me feel like I\'m doing accounting.', name: 'Maya R.', role: 'Book club organizer' },
]

function Testimonials() {
  return (
    <section style={{ padding: '80px 40px', background: 'white', borderTop: '1px solid var(--border-subtle)' }}>
      <div style={{ maxWidth: 1100, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: 48 }}>
          <h2 style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 'clamp(28px, 3.5vw, 44px)', letterSpacing: '-0.03em', margin: 0 }}>
            People love it.
          </h2>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 24 }}>
          {TESTIMONIALS.map(t => (
            <div
              key={t.name}
              style={{
                background: 'var(--sm-paper)', borderRadius: 20, padding: 28,
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div style={{ fontSize: 20, marginBottom: 12 }}>★★★★★</div>
              <p style={{ fontSize: 16, lineHeight: 1.6, margin: '0 0 20px', fontStyle: 'italic', color: 'var(--fg1)' }}>
                "{t.quote}"
              </p>
              <div style={{ fontWeight: 700, fontSize: 14 }}>{t.name}</div>
              <div style={{ fontSize: 12, color: 'var(--fg3)' }}>{t.role}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

const PLANS = [
  {
    name: 'Free',
    price: '₹0',
    period: 'forever',
    features: ['Up to 3 groups', 'Unlimited expenses', 'All split modes', 'Payment tracking'],
    cta: 'Get started',
    primary: false,
  },
  {
    name: 'Plus',
    price: '₹4',
    period: 'per month',
    features: ['Unlimited groups', 'Receipt OCR scanning', 'Multi-currency', 'Priority support', 'Export to CSV'],
    cta: 'Start free trial',
    primary: true,
    badge: 'Most popular',
  },
  {
    name: 'Team',
    price: '₹12',
    period: 'per month',
    features: ['Everything in Plus', 'Up to 20 members/group', 'Admin controls', 'Custom categories', 'SSO'],
    cta: 'Contact us',
    primary: false,
  },
]

function Pricing() {
  const navigate = useNavigate()
  return (
    <section style={{ padding: '80px 40px', background: 'var(--sm-paper)', borderTop: '1px solid var(--border-subtle)' }}>
      <div style={{ maxWidth: 1000, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: 60 }}>
          <h2 style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 'clamp(32px, 4vw, 52px)', letterSpacing: '-0.03em', margin: 0 }}>
            Simple pricing.
          </h2>
          <p style={{ color: 'var(--fg2)', fontSize: 18, marginTop: 16 }}>No tricks. Cancel anytime.</p>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20, alignItems: 'start' }}>
          {PLANS.map(p => (
            <div
              key={p.name}
              style={{
                borderRadius: 24, padding: 32,
                border: p.primary ? '2px solid var(--sm-green-500)' : '1px solid var(--border-default)',
                background: p.primary ? 'white' : 'white',
                boxShadow: p.primary ? 'var(--shadow-lg)' : 'var(--shadow-xs)',
                position: 'relative',
              }}
            >
              {p.badge && (
                <div
                  style={{
                    position: 'absolute', top: -14, left: '50%', transform: 'translateX(-50%)',
                    background: 'var(--grad-brand)', color: '#03241A', padding: '4px 16px',
                    borderRadius: 999, fontSize: 12, fontWeight: 700, whiteSpace: 'nowrap',
                  }}
                >
                  {p.badge}
                </div>
              )}
              <div style={{ fontSize: 16, fontWeight: 700, marginBottom: 8 }}>{p.name}</div>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginBottom: 4 }}>
                <span style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 44, letterSpacing: '-0.03em' }}>{p.price}</span>
                <span style={{ color: 'var(--fg3)', fontSize: 14 }}>{p.period}</span>
              </div>
              <div style={{ height: 1, background: 'var(--border-subtle)', margin: '20px 0' }} />
              <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 24px', display: 'flex', flexDirection: 'column', gap: 12 }}>
                {p.features.map(f => (
                  <li key={f} style={{ display: 'flex', gap: 10, fontSize: 14 }}>
                    <span style={{ color: 'var(--sm-green-500)', fontWeight: 700 }}>✓</span>
                    {f}
                  </li>
                ))}
              </ul>
              <button
                onClick={() => navigate('/signup')}
                style={{
                  width: '100%', padding: '13px', borderRadius: 999, border: p.primary ? 'none' : '1.5px solid var(--border-strong)',
                  background: p.primary ? 'var(--grad-brand)' : 'transparent', cursor: 'pointer',
                  fontWeight: 700, fontSize: 15, color: p.primary ? '#03241A' : 'var(--fg1)',
                  boxShadow: p.primary ? '0 4px 12px rgba(18,179,94,.32)' : 'none',
                }}
              >
                {p.cta}
              </button>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

const FAQS = [
  { q: 'Is it really free?', a: 'Yes. The free tier is genuinely free, forever. No credit card required.' },
  { q: 'How does settling up work?', a: 'SplitMoney shows you simplified payments — the minimum number of transfers needed to clear everyone\'s balance. You pay via Venmo, Cash App, PayPal, or mark it as cash.' },
  { q: 'Can I use it for trips abroad?', a: 'Yes — Plus and Team plans support multi-currency. Add expenses in any currency and we\'ll convert based on the date of the expense.' },
  { q: 'What if I already use Splitwise?', a: 'You can import your data. We make the move easy.' },
]

function FAQ() {
  const [open, setOpen] = useState(null)
  return (
    <section style={{ padding: '80px 40px', background: 'white', borderTop: '1px solid var(--border-subtle)' }}>
      <div style={{ maxWidth: 700, margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: 48 }}>
          <h2 style={{ fontFamily: 'var(--font-display)', fontWeight: 800, fontSize: 'clamp(28px, 3.5vw, 44px)', letterSpacing: '-0.03em', margin: 0 }}>
            Questions.
          </h2>
        </div>
        {FAQS.map((faq, i) => (
          <div
            key={i}
            style={{ borderBottom: '1px solid var(--border-subtle)' }}
          >
            <button
              onClick={() => setOpen(open === i ? null : i)}
              style={{
                width: '100%', padding: '20px 0', background: 'none', border: 'none', cursor: 'pointer',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 20,
              }}
            >
              <span style={{ fontSize: 16, fontWeight: 600, textAlign: 'left', color: 'var(--fg1)' }}>{faq.q}</span>
              <Icon name={open === i ? 'chevronDown' : 'chevronRight'} size={18} color="var(--fg3)" />
            </button>
            {open === i && (
              <div style={{ padding: '0 0 20px', fontSize: 15, color: 'var(--fg2)', lineHeight: 1.7 }}>
                {faq.a}
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  )
}

function CTA() {
  const navigate = useNavigate()
  return (
    <section
      style={{
        padding: '80px 40px',
        background: 'var(--sm-ink-950)',
        color: 'white',
        textAlign: 'center',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      <div style={{ position: 'absolute', inset: '-10%', background: 'var(--grad-glow)', opacity: 0.8 }} />
      <div style={{ position: 'relative', maxWidth: 640, margin: '0 auto' }}>
        <h2
          style={{
            fontFamily: 'var(--font-display)', fontWeight: 800,
            fontSize: 'clamp(36px, 5vw, 60px)', letterSpacing: '-0.04em',
            lineHeight: 1.05, margin: '0 0 20px',
          }}
        >
          Ready to stop<br />the spreadsheet?
        </h2>
        <p style={{ fontSize: 18, opacity: 0.75, marginBottom: 40 }}>
          Set up your first group in 30 seconds. It's free.
        </p>
        <button
          onClick={() => navigate('/signup')}
          style={{
            padding: '16px 40px', borderRadius: 999, border: 'none',
            background: 'var(--sm-lime-400)', cursor: 'pointer',
            fontWeight: 700, fontSize: 18, color: '#03241A',
            boxShadow: '0 8px 32px rgba(168,232,20,.4)',
          }}
        >
          Get started free →
        </button>
      </div>
    </section>
  )
}

function Footer() {
  return (
    <footer
      style={{
        background: 'var(--sm-ink-950)', borderTop: '1px solid rgba(255,255,255,0.06)',
        padding: '40px', color: 'rgba(255,255,255,0.5)', fontSize: 13,
        display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 16,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ width: 28, height: 28, borderRadius: 8, background: 'var(--grad-brand)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 800, fontSize: 16, color: '#03241A' }}>₹</div>
        <span style={{ color: 'white', fontWeight: 700 }}>SplitMoney</span>
      </div>
      <div>© 2026 SplitMoney, Inc. · <a href="#" style={{ color: 'inherit' }}>Privacy</a> · <a href="#" style={{ color: 'inherit' }}>Terms</a></div>
    </footer>
  )
}

export default function Landing() {
  return (
    <div style={{ minHeight: '100vh' }}>
      <NavBar />
      <Hero />
      <Features />
      <HowItWorks />
      <Testimonials />
      <Pricing />
      <FAQ />
      <CTA />
      <Footer />
    </div>
  )
}
