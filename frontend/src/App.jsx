import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'

const navigation = [
  { label: 'Overview', icon: 'grid' },
  { label: 'My accounts', icon: 'wallet' },
  { label: 'Transfers', icon: 'transfer' },
  { label: 'Activity', icon: 'activity' },
]

const accountTypes = [
  { value: 'CHECKING', label: 'Checking' },
  { value: 'SAVINGS', label: 'Savings' },
]

const money = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
})

const dateTime = new Intl.DateTimeFormat('en-IN', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const todayLabel = new Intl.DateTimeFormat('en-IN', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
}).format(Date.now()).toUpperCase()
const currentYear = new Date(Date.now()).getFullYear()

function Icon({ name, size = 20 }) {
  const paths = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="3" width="7" height="7" rx="1.5" /><rect x="14" y="14" width="7" height="7" rx="1.5" /><rect x="3" y="14" width="7" height="7" rx="1.5" /></>,
    wallet: <><rect x="3" y="5" width="18" height="15" rx="2.5" /><path d="M3 9h18M16 15h.01" /><path d="M6 5V4a2 2 0 0 1 2-2h10" /></>,
    transfer: <><path d="M7 7h13l-3-3M20 7l-3 3M17 17H4l3 3m-3-3 3-3" /></>,
    activity: <><path d="M3 12h4l3-8 4 16 3-8h4" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="m19.4 15 .1.1 1.4 1.1-1.4 2.4-1.7-.7a8 8 0 0 1-1.7 1l-.3 1.9h-2.8l-.3-1.9a8 8 0 0 1-1.7-1l-1.7.7-1.4-2.4 1.4-1.1a7 7 0 0 1 0-2L5.9 12l1.4-2.4 1.7.7a8 8 0 0 1 1.7-1l.3-1.9h2.8l.3 1.9a8 8 0 0 1 1.7 1l1.7-.7 1.4 2.4-1.4 1.1a7 7 0 0 1-.1 1.9Z" transform="translate(-1 -1)" /></>,
    help: <><circle cx="12" cy="12" r="9" /><path d="M9.6 9a2.5 2.5 0 1 1 4.2 1.8c-1.1 1-1.8 1.3-1.8 2.7M12 17h.01" /></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></>,
    search: <><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></>,
    eye: <><path d="M2 12s3.6-6 10-6 10 6 10 6-3.6 6-10 6S2 12 2 12Z" /><circle cx="12" cy="12" r="2.5" /></>,
    plus: <><path d="M12 5v14M5 12h14" /></>,
    arrow: <><path d="M7 17 17 7M7 7h10v10" /></>,
    arrowDown: <><path d="M12 4v15m-6-6 6 6 6-6" /></>,
    more: <><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></>,
    cart: <><path d="M3 4h2l2.3 11h10.9L21 8H6" /><circle cx="9" cy="19" r="1" /><circle cx="18" cy="19" r="1" /></>,
    play: <><rect x="3" y="5" width="18" height="14" rx="3" /><path d="m10 9 5 3-5 3z" /></>,
    coffee: <><path d="M5 8h13v7a4 4 0 0 1-4 4H9a4 4 0 0 1-4-4V8Zm13 2h2a2 2 0 1 1 0 4h-2M8 4v2m4-2v2m4-2v2" /></>,
    shield: <><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" /><path d="m9 12 2 2 4-4" /></>,
    chevron: <path d="m9 18 6-6-6-6" />,
    close: <><path d="m18 6-12 12M6 6l12 12" /></>,
    logout: <><path d="M10 17l5-5-5-5m5 5H3" /><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6" /></>,
  }

  return <svg aria-hidden="true" width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers)
  if (options.body) headers.set('Content-Type', 'application/json')
  if (options.method && options.method !== 'GET') {
    const csrfResponse = await fetch('/api/auth/csrf', { credentials: 'same-origin' })
    if (!csrfResponse.ok) throw new Error('Could not prepare a secure request. Please try again.')
    const { token } = await csrfResponse.json()
    headers.set('X-XSRF-TOKEN', token)
  }

  const response = await fetch(path, { ...options, headers, credentials: 'same-origin' })
  const body = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    if (response.status === 401) throw new Error('Your session has expired. Please sign in again.')
    throw new Error(body?.message || body?.error || `Request failed (${response.status}).`)
  }
  return body
}

function FormField({ label, ...props }) {
  return <label className="form-field"><span>{label}</span><input {...props} /></label>
}

function AuthScreen({ onAuthenticated, initialError }) {
  const [mode, setMode] = useState('login')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [form, setForm] = useState({ fullName: '', email: '', password: '', accountType: 'CHECKING' })

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const customer = await request(`/api/auth/${mode === 'login' ? 'login' : 'register'}`, {
        method: 'POST',
        body: JSON.stringify(mode === 'login'
          ? { email: form.email, password: form.password }
          : form),
      })
      onAuthenticated(customer)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-brand"><span className="brand-mark"><Icon name="grid" size={21} /></span><span className="brand-name">northstar<span>bank</span></span></div>
      <section className="auth-card">
        <div className="auth-icon"><Icon name="wallet" size={23} /></div>
        <span className="auth-eyebrow">YOUR MONEY, IN GOOD HANDS</span>
        <h1>{mode === 'login' ? 'Welcome back' : 'Create your account'}</h1>
        <p>{mode === 'login' ? 'Sign in to securely access your banking.' : 'Get started with a secure personal banking account.'}</p>
        <form onSubmit={submit}>
          {mode === 'register' && <>
            <FormField label="Full name" autoComplete="name" maxLength={90} required value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} />
            <label className="form-field"><span>Account type</span><select value={form.accountType} onChange={(event) => setForm({ ...form, accountType: event.target.value })}>{accountTypes.map((type) => <option value={type.value} key={type.value}>{type.label}</option>)}</select></label>
          </>}
          {initialError && !error && <div className="form-error" role="alert">{initialError}</div>}
          <FormField label="Email address" type="email" autoComplete="email" maxLength={254} required value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
          <FormField label="Password" type="password" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} minLength={mode === 'login' ? undefined : 10} maxLength={72} required value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
          {error && <div className="form-error" role="alert">{error}</div>}
          <button className="primary-button auth-submit" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Sign in securely' : 'Create account'}</button>
        </form>
        <div className="auth-switch">{mode === 'login' ? 'New to Northstar?' : 'Already have an account?'} <button onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError('') }}>{mode === 'login' ? 'Create an account' : 'Sign in'}</button></div>
        <div className="auth-security"><Icon name="shield" size={15} /> Your information is encrypted and protected.</div>
      </section>
      <p className="auth-disclaimer">Northstar Bank is a sample application. Use a test account only.</p>
    </main>
  )
}

function TransactionDialog({ accounts, kind, onClose, onSubmit, busy, error, initialAccountNumber }) {
  const [form, setForm] = useState({
    fromAccountNumber: accounts[0]?.accountNumber || '',
    toAccountNumber: accounts.find((account) => account.accountNumber !== accounts[0]?.accountNumber)?.accountNumber || '',
    accountNumber: initialAccountNumber || accounts[0]?.accountNumber || '',
    amount: '',
    description: '',
    type: 'SAVINGS',
    name: '',
  })
  const [localError, setLocalError] = useState('')
  const isTransfer = kind === 'transfer'
  const isDeposit = kind === 'deposit'
  const isCreate = kind === 'create'

  function submit(event) {
    event.preventDefault()
    const amount = Number(form.amount)
    if (!isCreate && (!Number.isFinite(amount) || amount <= 0)) {
      setLocalError('Enter an amount greater than ₹0.00.')
      return
    }
    if (isTransfer && form.fromAccountNumber === form.toAccountNumber) {
      setLocalError('Choose two different accounts.')
      return
    }
    setLocalError('')
    onSubmit(form)
  }

  return (
    <div className="dialog-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}>
      <section className="transaction-dialog" role="dialog" aria-modal="true" aria-labelledby="dialog-title">
        <div className="dialog-heading"><div><span className="auth-eyebrow">{isTransfer ? 'MOVE MONEY' : isDeposit ? 'ADD FUNDS' : 'NEW ACCOUNT'}</span><h2 id="dialog-title">{isTransfer ? 'Transfer money' : isDeposit ? 'Deposit funds' : 'Create an account'}</h2></div><button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div>
        <form onSubmit={submit}>
          {isTransfer && <>
            <label className="form-field"><span>From account</span><select required value={form.fromAccountNumber} onChange={(event) => setForm({ ...form, fromAccountNumber: event.target.value })}>{accounts.map((account) => <option key={account.id} value={account.accountNumber}>{account.name} · {money.format(Number(account.balance))} · •• {account.accountNumber.slice(-4)}</option>)}</select></label>
            <FormField label="Recipient account number" inputMode="numeric" pattern="[0-9]{12}" minLength={12} maxLength={12} required value={form.toAccountNumber} onChange={(event) => setForm({ ...form, toAccountNumber: event.target.value.replace(/\D/g, '') })} />
          </>}
          {isDeposit && <label className="form-field"><span>Deposit to</span><select required value={form.accountNumber} onChange={(event) => setForm({ ...form, accountNumber: event.target.value })}>{accounts.map((account) => <option key={account.id} value={account.accountNumber}>{account.name} · •• {account.accountNumber.slice(-4)}</option>)}</select></label>}
          {isCreate && <>
            <label className="form-field"><span>Account type</span><select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value })}>{accountTypes.map((type) => <option value={type.value} key={type.value}>{type.label}</option>)}</select></label>
            <FormField label="Account name" required maxLength={40} value={form.name} placeholder="e.g. Holiday savings" onChange={(event) => setForm({ ...form, name: event.target.value })} />
          </>}
          {!isCreate && <FormField label="Amount (INR ₹)" type="number" min="0.01" max="9999999999.99" step="0.01" inputMode="decimal" required autoFocus value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} />}
          {!isCreate && <FormField label="Description (optional)" maxLength={140} value={form.description} placeholder={isTransfer ? 'What is this transfer for?' : 'Add a note'} onChange={(event) => setForm({ ...form, description: event.target.value })} />}
          {(error || localError) && <div className="form-error" role="alert">{localError || error}</div>}
          <div className="dialog-actions"><button type="button" className="secondary-button" onClick={onClose}>Cancel</button><button className="primary-button" disabled={busy}>{busy ? 'Processing…' : isTransfer ? 'Transfer money' : isDeposit ? 'Deposit funds' : 'Create account'}</button></div>
        </form>
      </section>
    </div>
  )
}

function App() {
  const [activePage, setActivePage] = useState('Overview')
  const [balanceVisible, setBalanceVisible] = useState(true)
  const [notice, setNotice] = useState('')
  const [menuOpen, setMenuOpen] = useState(false)
  const [customer, setCustomer] = useState(null)
  const [authLoading, setAuthLoading] = useState(true)
  const [loading, setLoading] = useState(false)
  const [accounts, setAccounts] = useState([])
  const [transactions, setTransactions] = useState([])
  const [error, setError] = useState('')
  const [dialog, setDialog] = useState('')
  const [dialogBusy, setDialogBusy] = useState(false)
  const [dialogError, setDialogError] = useState('')
  const [dialogAccountNumber, setDialogAccountNumber] = useState('')

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [accountData, transactionData] = await Promise.all([
        request('/api/accounts'),
        request('/api/transactions?limit=30'),
      ])
      setAccounts(accountData.accounts)
      setTransactions(transactionData)
    } catch (requestError) {
      setError(requestError.message)
      if (requestError.message.startsWith('Your session has expired')) setCustomer(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let active = true
    request('/api/auth/me')
      .then(async (currentCustomer) => {
        if (!active) return
        setCustomer(currentCustomer)
        await loadDashboard()
      })
      .catch((requestError) => {
        if (active && !requestError.message.startsWith('Your session has expired')) setError(requestError.message)
      })
      .finally(() => { if (active) setAuthLoading(false) })
    return () => { active = false }
  }, [loadDashboard])

  const totalBalance = useMemo(
    () => accounts.reduce((total, account) => total + Number(account.balance), 0),
    [accounts],
  )

  const monthlyActivity = useMemo(() => {
    const months = new Map()
    for (const transaction of transactions) {
      const date = new Date(transaction.createdAt)
      if (Number.isNaN(date.getTime())) continue
      const month = date.toLocaleDateString('en-IN', { month: 'short' }).toUpperCase()
      const item = months.get(month) || { incoming: 0, outgoing: 0 }
      if (transaction.type === 'TRANSFER_OUT') item.outgoing += Number(transaction.amount)
      else item.incoming += Number(transaction.amount)
      months.set(month, item)
    }
    return [...months.entries()].reverse().slice(-7)
  }, [transactions])

  function showNotice(message) {
    setNotice(message)
    window.setTimeout(() => setNotice(''), 3000)
  }

  async function handleAuthenticated(currentCustomer) {
    setCustomer(currentCustomer)
    setActivePage('Overview')
    setError('')
    await loadDashboard()
  }

  async function signOut() {
    try {
      await request('/api/auth/logout', { method: 'POST' })
      setCustomer(null)
      setAccounts([])
      setTransactions([])
      setError('')
    } catch (requestError) {
      setError(requestError.message)
    }
  }

  async function submitDialog(form) {
    setDialogBusy(true)
    setDialogError('')
    try {
      if (dialog === 'transfer') {
        await request('/api/transfers', {
          method: 'POST',
          body: JSON.stringify({
            fromAccountNumber: form.fromAccountNumber,
            toAccountNumber: form.toAccountNumber,
            amount: Number(form.amount),
            description: form.description,
          }),
        })
        showNotice('Your transfer was completed successfully.')
      } else if (dialog === 'deposit') {
        await request(`/api/accounts/${form.accountNumber}/deposits`, {
          method: 'POST',
          body: JSON.stringify({ amount: Number(form.amount), description: form.description }),
        })
        showNotice('Your deposit was recorded successfully.')
      } else {
        await request('/api/accounts', {
          method: 'POST',
          body: JSON.stringify({ type: form.type, name: form.name }),
        })
        showNotice('Your new account is ready.')
      }
      setDialog('')
      await loadDashboard()
    } catch (requestError) {
      setDialogError(requestError.message)
    } finally {
      setDialogBusy(false)
    }
  }

  if (authLoading) {
    return <main className="auth-page"><div className="loading-screen"><span className="loading-spinner" />Checking your secure session…</div></main>
  }
  if (!customer) return <AuthScreen onAuthenticated={handleAuthenticated} initialError={error} />

  const initial = customer.fullName.split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase()
  const checkingAccounts = accounts.filter((account) => account.type === 'CHECKING')
  const savingsAccounts = accounts.filter((account) => account.type === 'SAVINGS')
  const accountBalance = (items) => items.reduce((sum, item) => sum + Number(item.balance), 0)

  return (
    <div className="banking-app">
      <aside className={`sidebar ${menuOpen ? 'sidebar-open' : ''}`}>
        <a className="brand" href="#overview" onClick={() => setActivePage('Overview')}>
          <span className="brand-mark"><Icon name="grid" size={21} /></span>
          <span className="brand-name">northstar<span>bank</span></span>
        </a>
        <div className="workspace-label">WORKSPACE</div>
        <nav className="main-nav" aria-label="Main navigation">
          {navigation.map(({ label, icon }) => (
            <button key={label} className={`nav-link ${activePage === label ? 'nav-active' : ''}`} onClick={() => { setActivePage(label); setMenuOpen(false) }}>
              <Icon name={icon} size={19} /><span>{label}</span>
              {label === 'Activity' && transactions.length > 0 && <span className="nav-count">{transactions.length}</span>}
            </button>
          ))}
        </nav>
        <div className="sidebar-spacer" />
        <div className="sidebar-support"><div className="support-icon"><Icon name="shield" size={20} /></div><strong>Your money, protected</strong><span>Bank securely with 24/7 monitoring.</span><button onClick={() => showNotice('Always keep your sign-in details private.')}>Security tips <Icon name="arrow" size={14} /></button></div>
        <div className="sidebar-bottom">
          <button className="nav-link" onClick={() => showNotice('Account settings are coming soon.')}><Icon name="settings" size={19} /><span>Settings</span></button>
          <button className="nav-link" onClick={signOut}><Icon name="logout" size={19} /><span>Sign out</span></button>
          <div className="profile"><div className="avatar">{initial}</div><div className="profile-info"><strong>{customer.fullName}</strong><span>{customer.email}</span></div><button className="icon-button profile-more" aria-label="Sign out" onClick={signOut}><Icon name="logout" size={17} /></button></div>
        </div>
      </aside>
      {menuOpen && <button className="mobile-scrim" aria-label="Close navigation" onClick={() => setMenuOpen(false)} />}

      <main className="main-content" id="overview">
        <header className="topbar">
          <button className="mobile-menu icon-button" aria-label="Open navigation" onClick={() => setMenuOpen(true)}><Icon name="grid" /></button>
          <div className="breadcrumb">Personal banking <Icon name="chevron" size={15} /> <span>{activePage}</span></div>
          <div className="topbar-actions"><span className="live-pill"><span /> LIVE ACCOUNT</span><button className="icon-button search-button" aria-label="Refresh accounts" onClick={loadDashboard}><Icon name="search" /></button><button className="icon-button notification-button" aria-label="Notifications" onClick={() => showNotice('You’re all caught up on notifications.')}><Icon name="bell" /></button><div className="topbar-divider" /><div className="avatar avatar-small">{initial}</div></div>
        </header>

        <div className="page-content">
          <section className="welcome-row">
            <div><div className="eyebrow">{todayLabel}</div><h1>{activePage === 'Overview' ? `Welcome, ${customer.fullName.split(' ')[0]}` : activePage}</h1><p className="welcome-subtitle">{activePage === 'Overview' ? 'Here’s what’s happening with your money today.' : `Your ${activePage.toLowerCase()} at a glance.`}</p></div>
            <div className="welcome-actions"><button className="secondary-button" onClick={() => { setDialogAccountNumber(''); setDialogError(''); setDialog('deposit') }} disabled={accounts.length === 0}><Icon name="arrowDown" size={17} /> Add funds</button><button className="primary-button" onClick={() => { setDialogError(''); setDialog('transfer') }} disabled={accounts.length === 0}><Icon name="transfer" size={17} /> New transfer</button></div>
          </section>

          {error && <div className="api-error" role="alert"><span>{error}</span><button onClick={loadDashboard}>Try again</button></div>}

          <section className="summary-grid" aria-label="Account summary">
            <article className="summary-card total-card"><div className="card-topline"><span className="card-label">TOTAL BALANCE</span><button className="balance-toggle" aria-label={balanceVisible ? 'Hide balance' : 'Show balance'} onClick={() => setBalanceVisible(!balanceVisible)}><Icon name="eye" size={17} /></button></div><div className="balance-value">{balanceVisible ? money.format(totalBalance) : '••••••••'}</div><div className="balance-foot"><span>{accounts.length} {accounts.length === 1 ? 'account' : 'accounts'}</span><span>across your bank</span></div><div className="balance-decoration decoration-one" /><div className="balance-decoration decoration-two" /></article>
            <article className="summary-card"><div className="metric-heading"><span className="metric-icon checking"><Icon name="wallet" size={18} /></span><span className="card-label">CHECKING</span><button className="icon-button metric-more" aria-label="Create account" onClick={() => { setDialogError(''); setDialog('create') }}><Icon name="plus" size={18} /></button></div><div className="metric-value">{balanceVisible ? money.format(accountBalance(checkingAccounts)) : '••••••'}</div><div className="account-number">{checkingAccounts.length ? `${checkingAccounts.length} checking ${checkingAccounts.length === 1 ? 'account' : 'accounts'}` : 'No checking account'}</div></article>
            <article className="summary-card"><div className="metric-heading"><span className="metric-icon savings"><Icon name="activity" size={18} /></span><span className="card-label">SAVINGS</span><button className="icon-button metric-more" aria-label="Create account" onClick={() => { setDialogError(''); setDialog('create') }}><Icon name="plus" size={18} /></button></div><div className="metric-value">{balanceVisible ? money.format(accountBalance(savingsAccounts)) : '••••••'}</div><div className="account-number">{savingsAccounts.length ? `${savingsAccounts.length} savings ${savingsAccounts.length === 1 ? 'account' : 'accounts'}` : 'No savings account'}</div></article>
          </section>

          {activePage === 'Overview' && <section className="overview-grid">
            <article className="panel spending-panel"><div className="panel-heading"><div><h2>Account activity</h2><p>Credits and transfers from your recent transactions</p></div><button className="select-button" onClick={loadDashboard}>{loading ? 'Updating…' : 'Refresh'} <span>↻</span></button></div>
              {monthlyActivity.length > 0 ? <><div className="chart-legend"><span><i className="legend-income" /> Credits & deposits</span><span><i className="legend-spending" /> Transfers out</span><strong>NET BALANCE <b>{money.format(totalBalance)}</b></strong></div><div className="chart-wrap"><div className="chart-y-labels"><span>More</span><span /><span /><span /><span>Less</span></div><div className="activity-bars">{monthlyActivity.map(([month, totals]) => { const max = Math.max(...monthlyActivity.map(([, value]) => Math.max(value.incoming, value.outgoing)), 1); return <div className="activity-month" key={month}><div className="activity-bar-pair"><span className="activity-bar income-bar" style={{ height: `${Math.max(5, totals.incoming / max * 100)}%` }} title={`Credits ${money.format(totals.incoming)}`} /><span className="activity-bar spending-bar" style={{ height: `${Math.max(5, totals.outgoing / max * 100)}%` }} title={`Transfers ${money.format(totals.outgoing)}`} /></div><span>{month}</span></div> })}</div></div></> : <div className="empty-chart"><span className="metric-icon checking"><Icon name="activity" /></span><strong>Your activity will appear here</strong><span>Deposits and transfers will be summarized once you start using your account.</span></div>}
            </article>
            <article className="panel savings-panel"><div className="panel-heading"><div><h2>Your accounts</h2><p>Balances in Indian rupees</p></div><button className="icon-button" aria-label="Create account" onClick={() => { setDialogError(''); setDialog('create') }}><Icon name="plus" /></button></div>{accounts.length ? <div className="account-list">{accounts.map((account) => <div className="account-list-row" key={account.id}><span className={`metric-icon ${account.type === 'SAVINGS' ? 'savings' : 'checking'}`}><Icon name={account.type === 'SAVINGS' ? 'activity' : 'wallet'} size={17} /></span><div className="account-list-info"><strong>{account.name}</strong><span>{account.type} · •• {account.accountNumber.slice(-4)}</span></div><b>{money.format(Number(account.balance))}</b></div>)}</div> : <div className="account-empty">Create an account to get started.</div>}<button className="text-button" onClick={() => { setDialogError(''); setDialog('create') }}>Open an account <Icon name="arrow" size={15} /></button></article>
          </section>}

          {activePage === 'My accounts' && <section className="panel page-panel"><div className="panel-heading"><div><h2>My accounts</h2><p>Your accounts and balances in Indian rupees</p></div><button className="primary-button" onClick={() => { setDialogError(''); setDialog('create') }}><Icon name="plus" size={17} /> Open account</button></div>{accounts.map((account) => <div className="account-detail-row" key={account.id}><div className={`metric-icon ${account.type === 'SAVINGS' ? 'savings' : 'checking'}`}><Icon name={account.type === 'SAVINGS' ? 'activity' : 'wallet'} /></div><div className="account-list-info"><strong>{account.name}</strong><span>{account.type} account · {account.accountNumber}</span></div><b>{money.format(Number(account.balance))}</b><button className="secondary-button" onClick={() => { setDialogAccountNumber(account.accountNumber); setDialogError(''); setDialog('deposit') }}>Add funds</button></div>)}</section>}

          {(activePage === 'Activity' || activePage === 'Transfers') && <section className="panel transactions-panel page-panel"><div className="panel-heading transactions-heading"><div><h2>{activePage === 'Transfers' ? 'Transfers' : 'Recent transactions'}</h2><p>Real account activity · amounts shown in INR</p></div>{activePage === 'Transfers' && <button className="primary-button" onClick={() => { setDialogError(''); setDialog('transfer') }}><Icon name="transfer" size={17} /> New transfer</button>}</div><TransactionList transactions={transactions} onSelect={(transaction) => showNotice(`${transaction.description || transaction.type} · ${money.format(Number(transaction.amount))}`)} /></section>}

          {activePage === 'Overview' && <section className="panel transactions-panel"><div className="panel-heading transactions-heading"><div><h2>Recent transactions</h2><p>Your latest account activity</p></div><button className="view-all" onClick={() => setActivePage('Activity')}>View all activity <Icon name="arrow" size={15} /></button></div><TransactionList transactions={transactions.slice(0, 5)} onSelect={(transaction) => showNotice(`${transaction.description || transaction.type} · ${money.format(Number(transaction.amount))}`)} /></section>}
          <footer className="page-footer"><span>© {currentYear} Northstar Bank. Sample application.</span><span><Icon name="shield" size={14} /> Account balances and transactions are private</span></footer>
        </div>
      </main>
      {dialog && <TransactionDialog accounts={accounts} kind={dialog} initialAccountNumber={dialogAccountNumber} onClose={() => setDialog('')} onSubmit={submitDialog} busy={dialogBusy} error={dialogError} />}
      {notice && <div className="toast" role="status"><span className="toast-check">✓</span>{notice}<button aria-label="Dismiss message" onClick={() => setNotice('')}><Icon name="close" size={15} /></button></div>}
    </div>
  )
}

function TransactionList({ transactions, onSelect }) {
  if (transactions.length === 0) {
    return <div className="empty-transactions"><span className="metric-icon checking"><Icon name="activity" /></span><strong>No transactions yet</strong><span>Deposits and transfers will show up here when they happen.</span></div>
  }

  return <div className="transaction-list">{transactions.map((transaction) => {
    const incoming = transaction.type !== 'TRANSFER_OUT'
    const icon = transaction.type === 'DEPOSIT' ? 'arrowDown' : 'transfer'
    const color = transaction.type === 'DEPOSIT' ? 'mint' : incoming ? 'lavender' : 'blue'
    const title = transaction.description || (transaction.type === 'DEPOSIT' ? 'Deposit' : incoming ? 'Transfer received' : 'Transfer sent')
    return <button className="transaction-row transaction-row-button" key={transaction.id} onClick={() => onSelect(transaction)}><span className={`transaction-icon ${color}`}><Icon name={icon} size={19} /></span><span className="transaction-info"><strong>{title}</strong><span>{transaction.type.replaceAll('_', ' ')} · {dateTime.format(Date.parse(transaction.createdAt))} · •• {transaction.accountNumber.slice(-4)}</span></span><span className={`transaction-amount ${incoming ? 'amount-incoming' : ''}`}>{incoming ? '+' : '−'}{money.format(Number(transaction.amount))}</span><Icon name="chevron" size={16} /></button>
  })}</div>
}

export default App
