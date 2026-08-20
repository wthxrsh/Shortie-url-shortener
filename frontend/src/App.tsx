import { FormEvent, useMemo, useState } from 'react'

type ShortUrl = {
  id: number
  originalUrl: string
  shortCode: string
  createdAt: string
  expiresAt: string | null
  clickCount: number
}

type ApiError = { message?: string; error?: string }

const formatDate = (value: string | null) => value
  ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
  : 'Never'

async function getError(response: Response) {
  const body = await response.json().catch(() => ({})) as ApiError
  return body.message || body.error || 'Something went wrong. Please try again.'
}

export default function App() {
  const [originalUrl, setOriginalUrl] = useState('')
  const [expiresAt, setExpiresAt] = useState('')
  const [created, setCreated] = useState<ShortUrl | null>(null)
  const [lookup, setLookup] = useState('')
  const [stats, setStats] = useState<ShortUrl | null>(null)
  const [error, setError] = useState('')
  const [statsError, setStatsError] = useState('')
  const [loading, setLoading] = useState(false)
  const [lookingUp, setLookingUp] = useState(false)
  const [copied, setCopied] = useState(false)

  const shortLink = useMemo(() => created ? `${window.location.origin}/${created.shortCode}` : '', [created])

  const createUrl = async (event: FormEvent) => {
    event.preventDefault()
    setError('')
    setCreated(null)
    setLoading(true)
    try {
      const response = await fetch('/api/urls', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ originalUrl, ...(expiresAt ? { expiresAt: `${expiresAt}:00` } : {}) }),
      })
      if (!response.ok) throw new Error(await getError(response))
      setCreated(await response.json() as ShortUrl)
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Could not shorten this URL.')
    } finally { setLoading(false) }
  }

  const loadStats = async (event: FormEvent) => {
    event.preventDefault()
    const code = lookup.trim().split('/').filter(Boolean).pop() || ''
    setStatsError(''); setStats(null); setLookingUp(true)
    try {
      const response = await fetch(`/api/urls/${encodeURIComponent(code)}/stats`)
      if (!response.ok) throw new Error(await getError(response))
      setStats(await response.json() as ShortUrl)
    } catch (cause) {
      setStatsError(cause instanceof Error ? cause.message : 'Could not find that short link.')
    } finally { setLookingUp(false) }
  }

  const copyLink = async () => {
    await navigator.clipboard.writeText(shortLink)
    setCopied(true); window.setTimeout(() => setCopied(false), 1800)
  }

  return <main>
    <nav><a className="brand" href="/">short<span>ie</span></a><span className="tag">Simple links. Clear results.</span></nav>
    <section className="hero">
      <p className="eyebrow">A faster way to share</p>
      <h1>Make every link<br /><em>count.</em></h1>
      <p className="intro">Create a clean, shareable URL in seconds. Add an expiry when the moment matters.</p>
    </section>
    <section className="workspace" aria-label="URL tools">
      <article className="panel create-panel">
        <div className="panel-heading"><div><p className="eyebrow">New short link</p><h2>Drop in a long URL</h2></div><span className="step">01</span></div>
        <form onSubmit={createUrl}>
          <label htmlFor="url">Destination URL</label>
          <input id="url" type="url" placeholder="https://your-really-long-link.com" value={originalUrl} onChange={e => setOriginalUrl(e.target.value)} required />
          <div className="field-row"><div><label htmlFor="expires">Expires on <small>optional</small></label><input id="expires" type="datetime-local" value={expiresAt} onChange={e => setExpiresAt(e.target.value)} /></div><button className="primary" disabled={loading}>{loading ? 'Creating…' : 'Shorten URL →'}</button></div>
        </form>
        {error && <p className="message error" role="alert">{error}</p>}
        {created && <div className="result"><p>Your short link is ready</p><div className="short-link"><a href={shortLink}>{shortLink}</a><button onClick={copyLink}>{copied ? 'Copied!' : 'Copy'}</button></div><div className="result-meta"><span>Created {formatDate(created.createdAt)}</span><span>Expires {formatDate(created.expiresAt)}</span></div></div>}
      </article>
      <article className="panel stats-panel">
        <div className="panel-heading"><div><p className="eyebrow">Link insights</p><h2>Check performance</h2></div><span className="step">02</span></div>
        <form onSubmit={loadStats} className="lookup"><label htmlFor="lookup">Short code or link</label><div><input id="lookup" placeholder="e.g. aB3xY9" value={lookup} onChange={e => setLookup(e.target.value)} required /><button disabled={lookingUp}>{lookingUp ? 'Checking…' : 'View stats'}</button></div></form>
        {statsError && <p className="message error" role="alert">{statsError}</p>}
        {stats && <div className="stats-result"><div className="clicks"><strong>{stats.clickCount.toLocaleString()}</strong><span>total clicks</span></div><div className="stat-details"><p title={stats.originalUrl}>{stats.originalUrl}</p><span>Created {formatDate(stats.createdAt)}</span><span>Expires {formatDate(stats.expiresAt)}</span></div></div>}
        {!stats && !statsError && <p className="hint">Paste a short code to see its destination, expiry, and total clicks.</p>}
      </article>
    </section>
    <footer><span>Built for links that deserve a little less friction.</span><span>URL Shortener</span></footer>
  </main>
}
