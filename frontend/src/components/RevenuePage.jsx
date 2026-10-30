import { useEffect, useState } from 'react'
import { Activity, CalendarDays, ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { getAdminRevenueReport } from '../api/payments'
import '../styles/revenue.css'

const PAGE_SIZE = 25
const monthNames = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']

function money(paise, currency = 'INR') {
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 2 }).format(Number(paise || 0) / 100)
}

function paidDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function RevenuePage() {
  const now = new Date()
  const [period, setPeriod] = useState('MONTHLY')
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [page, setPage] = useState(0)
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError('')
    getAdminRevenueReport({ period, year, month, page, size: PAGE_SIZE, signal: controller.signal })
      .then((result) => { if (!controller.signal.aborted) setReport(result) })
      .catch((requestError) => { if (!controller.signal.aborted) setError(requestError.message) })
      .finally(() => { if (!controller.signal.aborted) setLoading(false) })
    return () => controller.abort()
  }, [period, year, month, page, reload])

  function changePeriod(nextPeriod) { setPeriod(nextPeriod); setPage(0) }

  const heading = period === 'MONTHLY' ? `${monthNames[month - 1]} ${year}` : `Year ${year}`

  return <div className="page-content revenue-page">
    <div className="page-title-row revenue-title-row">
      <div><p className="eyebrow">PAYMENT REPORTS</p><h1>Revenue</h1><p className="page-subtitle">Successful, verified member payments, grouped by the date they were paid.</p></div>
      <button className="button button-secondary" type="button" onClick={() => setReload((value) => value + 1)} disabled={loading}><RefreshCw size={16} /> Refresh</button>
    </div>

    <section className="revenue-controls surface" aria-label="Revenue report filters">
      <div className="revenue-period-switch" role="tablist" aria-label="Revenue period">
        <button type="button" role="tab" aria-selected={period === 'MONTHLY'} className={period === 'MONTHLY' ? 'active' : ''} onClick={() => changePeriod('MONTHLY')}>Monthly</button>
        <button type="button" role="tab" aria-selected={period === 'YEARLY'} className={period === 'YEARLY' ? 'active' : ''} onClick={() => changePeriod('YEARLY')}>Yearly</button>
      </div>
      {period === 'MONTHLY' && <label><span>Month</span><select value={month} onChange={(event) => { setMonth(Number(event.target.value)); setPage(0) }}>{monthNames.map((name, index) => <option key={name} value={index + 1}>{name}</option>)}</select></label>}
      <label><span>Year</span><input type="number" min="2000" max="9999" value={year} onChange={(event) => { const value = Number(event.target.value); if (value >= 2000 && value <= 9999) { setYear(value); setPage(0) } }} /></label>
    </section>

    <section className="revenue-summary-grid">
      <article className="surface revenue-total-card"><span className="revenue-icon"><Activity size={20} /></span><small>{period === 'MONTHLY' ? 'MONTHLY REVENUE' : 'YEARLY REVENUE'}</small><strong>{loading ? '…' : report ? money(report.totalAmountPaise, report.currency) : '—'}</strong><span>{heading}</span></article>
      <article className="surface revenue-count-card"><span className="revenue-icon"><CalendarDays size={20} /></span><small>SUCCESSFUL PAYMENTS</small><strong>{loading ? '…' : report?.successfulPaymentCount ?? '—'}</strong><span>{heading}</span></article>
    </section>

    <section className="surface revenue-transactions">
      <div className="revenue-table-heading"><div><h2>Payments</h2><p>{period === 'MONTHLY' ? 'Members and plans paid during this month.' : 'Members and plans paid during this year.'}</p></div>{report && <span>{report.successfulPaymentCount} payments</span>}</div>
      {error ? <div className="member-error" role="alert">{error}<button className="button button-secondary" type="button" onClick={() => setReload((value) => value + 1)}>Retry</button></div> : loading ? <div className="loading-state" role="status"><span className="loading-spinner" />Loading revenue report…</div> : !report?.payments?.length ? <p className="revenue-empty">No successful payments were recorded for {heading}.</p> : <>
        <div className="member-table-scroll revenue-table-scroll"><table className="revenue-table"><thead><tr><th>Member</th><th>Plan</th><th>Amount</th><th>Paid at</th><th>Payment</th></tr></thead><tbody>{report.payments.map((payment) => <tr key={payment.paymentId}><td><strong>{payment.memberName || `Member #${payment.memberId}`}</strong><small>{payment.memberEmail || `Member ID #${payment.memberId}`}</small></td><td>{payment.planName}</td><td>{money(payment.amountPaise, payment.currency)}</td><td>{paidDate(payment.paidAt)}</td><td><span className="revenue-success-status">SUCCESS</span></td></tr>)}</tbody></table></div>
        {report.totalPages > 1 && <div className="pagination revenue-pagination"><span>Page {report.page + 1} of {report.totalPages}</span><div><button type="button" className="button button-secondary" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}><ChevronLeft size={16} /> Previous</button><button type="button" className="button button-secondary" disabled={page + 1 >= report.totalPages} onClick={() => setPage((current) => current + 1)}>Next <ChevronRight size={16} /></button></div></div>}
      </>}
    </section>
  </div>
}
