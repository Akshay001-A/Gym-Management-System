import { useEffect, useRef, useState } from 'react'
import { ArrowLeft, BadgeCheck, CalendarDays, CreditCard, ExternalLink, RefreshCw, ShieldCheck } from 'lucide-react'
import { getMembershipPlans } from '../api/member'
import {
  createIdempotencyKey, createPaymentOrder, getPaymentDetails,
  getPaymentHistory, verifyPayment,
} from '../api/payments'

const PAGE_SIZE = 10
let checkoutScriptPromise

function loadCheckoutScript() {
  if (window.Razorpay) return Promise.resolve()
  if (!checkoutScriptPromise) {
    checkoutScriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = 'https://checkout.razorpay.com/v1/checkout.js'
      script.async = true
      script.onload = () => {
        if (window.Razorpay) resolve()
        else {
          checkoutScriptPromise = null
          reject(new Error('Razorpay Checkout did not load.'))
        }
      }
      script.onerror = () => {
        checkoutScriptPromise = null
        reject(new Error('Could not load Razorpay Checkout. Check your connection and try again.'))
      }
      document.body.appendChild(script)
    })
  }
  return checkoutScriptPromise
}

function messageFor(error) {
  if (error?.status === 403) return 'Your account does not have permission to make this payment.'
  if (error?.status === 400) return error.message || 'The payment request was invalid. Please review the selected plan.'
  if (error?.status === 404) return 'The Payment API returned 404. Rebuild and restart the Payment Service and API Gateway, then try again.'
  if (error?.status >= 500) return 'The payment service is temporarily unavailable. Please try again shortly.'
  return error?.message || 'The payment could not be completed.'
}

function formatAmount(amountPaise, currency = 'INR') {
  const value = Number(amountPaise)
  if (!Number.isFinite(value)) return '—'
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency }).format(value / 100)
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

function StatusBadge({ status }) {
  const normalized = String(status || 'UNKNOWN').toUpperCase()
  return <span className={`payment-status payment-status-${normalized.toLowerCase()}`}>{normalized.replaceAll('_', ' ')}</span>
}

export default function PaymentCenter({ initialPlan = '', session, onPlanConsumed, onMembershipUpdated, hasCurrentMembership = false }) {
  const [tab, setTab] = useState(initialPlan ? 'plans' : 'history')
  const [plans, setPlans] = useState([])
  const [plansLoading, setPlansLoading] = useState(true)
  const [plansError, setPlansError] = useState('')
  const [planReload, setPlanReload] = useState(0)
  const [selectedPlan, setSelectedPlan] = useState(initialPlan)
  const [order, setOrder] = useState(null)
  const [payment, setPayment] = useState(null)
  const [checkoutFailure, setCheckoutFailure] = useState(null)
  const [checkoutPending, setCheckoutPending] = useState(null)
  const [checkoutError, setCheckoutError] = useState('')
  const [checkoutBusy, setCheckoutBusy] = useState(false)
  const [checkoutMessage, setCheckoutMessage] = useState('')
  const busyRef = useRef(false)

  useEffect(() => {
    if (!initialPlan) return
    setSelectedPlan(initialPlan)
    setTab('plans')
    onPlanConsumed?.()
  }, [initialPlan, onPlanConsumed])

  useEffect(() => {
    const controller = new AbortController()
    setPlansLoading(true)
    getMembershipPlans({ signal: controller.signal })
      .then((data) => {
        setPlans(data.filter((plan) => ['MONTHLY', 'QUARTERLY', 'YEARLY'].includes(plan.name)))
        setPlansError('')
      })
      .catch((error) => { if (error.name !== 'AbortError') setPlansError(messageFor(error)) })
      .finally(() => { if (!controller.signal.aborted) setPlansLoading(false) })
    return () => controller.abort()
  }, [planReload])

  function resetCheckout() {
    setPayment(null)
    setCheckoutFailure(null)
    setCheckoutPending(null)
    setCheckoutError('')
    setCheckoutMessage('')
  }

  async function openCheckout(paymentOrder) {
    if (busyRef.current) return
    busyRef.current = true
    setCheckoutBusy(true)
    setCheckoutError('')
    setCheckoutMessage('Preparing secure checkout…')
    let handledOutcome = false
    try {
      if (!paymentOrder?.razorpayKeyId || !paymentOrder?.razorpayOrderId || !paymentOrder?.amountPaise) {
        throw new Error('The Payment Service returned incomplete order details.')
      }
      await loadCheckoutScript()
      const checkout = new window.Razorpay({
        key: paymentOrder.razorpayKeyId,
        amount: paymentOrder.amountPaise,
        currency: paymentOrder.currency,
        name: 'Form & Fit',
        description: `${paymentOrder.planName} membership`,
        order_id: paymentOrder.razorpayOrderId,
        prefill: { name: session.name || '', email: session.email || '' },
        theme: { color: '#247951' },
        modal: {
          ondismiss: () => {
            if (handledOutcome) return
            handledOutcome = true
            busyRef.current = false
            setCheckoutBusy(false)
            setCheckoutMessage('Checkout was closed. The server has not confirmed a completed payment.')
            setCheckoutPending({ order: paymentOrder, message: 'Check payment status before trying again.' })
          },
        },
        handler: async (response) => {
          handledOutcome = true
          setCheckoutMessage('Payment received by Checkout. Confirming it with the Payment Service…')
          try {
            if (response.razorpay_order_id !== paymentOrder.razorpayOrderId) {
              throw new Error('The Checkout order did not match the order created by the server.')
            }
            const verified = await verifyPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            })
            if (String(verified.status).toUpperCase() !== 'SUCCESS') {
              setCheckoutPending({ order: paymentOrder, message: 'The server has not confirmed this payment yet.' })
              setCheckoutMessage('Payment status is still being confirmed. Check the status again before retrying.')
              return
            }
            let actual = verified
            if (verified.paymentId) {
              try { actual = await getPaymentDetails(verified.paymentId) } catch { /* Keep the verified response if the follow-up read is unavailable. */ }
            }
            setPayment(actual)
            try { await onMembershipUpdated?.() } catch { /* Payment is already server-verified. */ }
            setOrder(null)
            setCheckoutPending(null)
            setCheckoutFailure(null)
            setCheckoutMessage('')
          } catch (error) {
            setCheckoutPending({ order: paymentOrder, message: messageFor(error) })
            setCheckoutMessage(`Checkout returned a payment response, but the server has not confirmed success. ${messageFor(error)}`)
          } finally {
            busyRef.current = false
            setCheckoutBusy(false)
          }
        },
      })
      checkout.on('payment.failed', (event) => {
        handledOutcome = true
        busyRef.current = false
        setCheckoutBusy(false)
        setOrder(null)
        setCheckoutPending(null)
        setCheckoutFailure({
          message: event?.error?.description || 'Razorpay did not complete this payment.',
          paymentId: event?.error?.metadata?.payment_id,
          orderId: paymentOrder.razorpayOrderId,
        })
        setCheckoutMessage('This payment attempt failed. You can try again with a new order.')
      })
      setOrder(paymentOrder)
      setCheckoutMessage('Complete the payment in the secure Razorpay window.')
      checkout.open()
    } catch (error) {
      busyRef.current = false
      setCheckoutBusy(false)
      setCheckoutError(messageFor(error))
      setCheckoutMessage('')
    }
  }

  async function beginPayment() {
    if (!selectedPlan || busyRef.current) return
    resetCheckout()
    busyRef.current = true
    setCheckoutBusy(true)
    setCheckoutMessage('Creating a secure payment order…')
    try {
      const created = await createPaymentOrder(selectedPlan, createIdempotencyKey())
      setOrder(created)
      busyRef.current = false
      setCheckoutBusy(false)
      await openCheckout(created)
    } catch (error) {
      busyRef.current = false
      setCheckoutBusy(false)
      setCheckoutMessage('')
      setCheckoutError(messageFor(error))
    }
  }

  async function refreshPaymentStatus(targetOrder = order || checkoutPending?.order) {
    if (!targetOrder?.paymentId || busyRef.current) return
    busyRef.current = true
    setCheckoutBusy(true)
    setCheckoutError('')
    setCheckoutMessage('Refreshing payment status from the server…')
    try {
      const latest = await getPaymentDetails(targetOrder.paymentId)
      const status = String(latest.status).toUpperCase()
      if (status === 'SUCCESS') {
        setPayment(latest)
        try { await onMembershipUpdated?.() } catch { /* Payment is already server-verified. */ }
        setCheckoutPending(null)
        setCheckoutFailure(null)
        setOrder(null)
        setCheckoutMessage('')
      } else if (status === 'FAILED') {
        setCheckoutFailure({ message: latest.failureReason || 'The Payment Service marked this attempt as failed.', paymentId: latest.razorpayPaymentId, orderId: latest.razorpayOrderId })
        setCheckoutPending(null)
        setOrder(null)
        setCheckoutMessage('The Payment Service confirmed that this attempt failed.')
      } else {
        setCheckoutPending({ order: targetOrder, message: 'The payment is still awaiting confirmation.' })
        setCheckoutMessage('The payment is still pending. Check again in a moment.')
      }
    } catch (error) {
      setCheckoutError(messageFor(error))
      setCheckoutMessage('')
    } finally {
      busyRef.current = false
      setCheckoutBusy(false)
    }
  }

  function retryPayment() {
    resetCheckout()
    setOrder(null)
    setCheckoutFailure(null)
    setTab('plans')
  }

  return <section className="member-payment-center">
    <div className="member-payment-heading">
      <div><p className="member-eyebrow">SECURE PAYMENTS</p><h2>Plans &amp; payments</h2><p>Choose a current club plan, pay securely, and review server-verified transactions.</p></div>
      <span><ShieldCheck size={24} /></span>
    </div>
    {!payment && !checkoutFailure && !checkoutPending && <nav className="payment-tabs" aria-label="Payment sections">
      <button type="button" className={tab === 'plans' ? 'selected' : ''} onClick={() => setTab('plans')}>Membership plans</button>
      <button type="button" className={tab === 'history' ? 'selected' : ''} onClick={() => setTab('history')}>Payment history</button>
    </nav>}

    {checkoutError && <div className="member-error" role="alert">{checkoutError}</div>}
    {hasCurrentMembership && !payment && <div className="payment-message" role="status">You have an active membership. Cancel it or wait for it to expire before choosing another plan.</div>}
    {checkoutMessage && !payment && <div className="payment-message" role="status">{checkoutBusy && <span className="loading-spinner" />}{checkoutMessage}</div>}

    {payment ? <PaymentSuccess payment={payment} onHistory={() => { setPayment(null); setTab('history') }} onPlans={retryPayment} /> : checkoutFailure ? <PaymentFailure failure={checkoutFailure} onRetry={retryPayment} onHistory={() => { setCheckoutFailure(null); setTab('history') }} /> : checkoutPending ? <PaymentPending pending={checkoutPending} busy={checkoutBusy} onRefresh={() => refreshPaymentStatus(checkoutPending.order)} onRetry={retryPayment} /> : tab === 'plans' ? <>
      {plansError ? <div className="member-error" role="alert">{plansError}<button type="button" onClick={() => setPlanReload((value) => value + 1)}>Retry</button></div> : plansLoading ? <div className="member-loading" role="status"><span className="loading-spinner" />Loading membership plans…</div> : plans.length === 0 ? <p className="member-empty">No payment plans are available right now.</p> : <div className="payment-plan-grid">
        {plans.map((plan) => <button type="button" disabled={hasCurrentMembership} className={`payment-plan-card ${selectedPlan === plan.name ? 'selected' : ''}`} key={plan.name} onClick={() => { setSelectedPlan(plan.name); resetCheckout() }}>
          <span className="payment-plan-check"><BadgeCheck size={18} /></span><span className="payment-plan-name">{plan.name}</span><strong>{formatAmount(Number(plan.price) * 100)}</strong><small>{plan.durationMonths} month{plan.durationMonths === 1 ? '' : 's'}</small>
        </button>)}
      </div>}
      {selectedPlan && <div className="payment-confirm-card">
        <div><span>Selected plan</span><strong>{selectedPlan}</strong><small>Plan price is supplied by the Membership Service. The payment amount is calculated again by the backend.</small></div>
        <button type="button" className="member-payment-primary" disabled={hasCurrentMembership || plansLoading || checkoutBusy || !plans.some((plan) => plan.name === selectedPlan)} onClick={beginPayment}>{hasCurrentMembership ? 'Plan already active' : checkoutBusy ? 'Preparing…' : 'Proceed to payment'}<CreditCard size={16} /></button>
      </div>}
      {order && !checkoutBusy && <div className="payment-retry-card"><span>Order #{order.paymentId} is ready. Reopen Checkout without creating another order.</span><button type="button" onClick={() => openCheckout(order)}>Open Checkout</button></div>}
      <p className="payment-security-note"><ShieldCheck size={15} />Payment confirmation is verified by the server. Your Razorpay secret is never sent to this page.</p>
    </> : <PaymentHistory onViewPlans={retryPayment} />}
  </section>
}

function PaymentSuccess({ payment, onHistory, onPlans }) {
  return <section className="payment-outcome payment-outcome-success" aria-labelledby="payment-success-title">
    <span className="payment-outcome-icon"><BadgeCheck size={27} /></span>
    <p className="member-eyebrow">SERVER VERIFIED</p><h3 id="payment-success-title">Payment successful</h3>
    <p>The Payment Service verified this transaction and activated your membership.</p>
    <PaymentFacts payment={payment} />
    <div className="payment-outcome-actions"><button type="button" className="member-payment-primary" onClick={onHistory}>View payment history</button><button type="button" className="member-payment-secondary" onClick={onPlans}>Back to plans</button></div>
  </section>
}

function PaymentFailure({ failure, onRetry, onHistory }) {
  return <section className="payment-outcome payment-outcome-failure" aria-labelledby="payment-failure-title">
    <span className="payment-outcome-icon"><CreditCard size={25} /></span>
    <p className="member-eyebrow">PAYMENT NOT COMPLETED</p><h3 id="payment-failure-title">This payment attempt failed</h3>
    <p>{failure.message}</p>
    {failure.paymentId && <dl className="payment-facts"><div><dt>Payment ID</dt><dd>{failure.paymentId}</dd></div>{failure.orderId && <div><dt>Order ID</dt><dd>{failure.orderId}</dd></div>}</dl>}
    <div className="payment-outcome-actions"><button type="button" className="member-payment-primary" onClick={onRetry}>Retry payment</button><button type="button" className="member-payment-secondary" onClick={onHistory}>Payment history</button></div>
  </section>
}

function PaymentPending({ pending, busy, onRefresh, onRetry }) {
  return <section className="payment-outcome payment-outcome-pending" aria-labelledby="payment-pending-title">
    <span className="payment-outcome-icon"><RefreshCw size={24} /></span>
    <p className="member-eyebrow">CONFIRMATION PENDING</p><h3 id="payment-pending-title">Check payment status</h3>
    <p>{pending.message} Do not start another payment until you have checked the transaction status.</p>
    <PaymentFacts payment={{ paymentId: pending.order?.paymentId, planName: pending.order?.planName, amountPaise: pending.order?.amountPaise, currency: pending.order?.currency, status: 'PENDING', razorpayOrderId: pending.order?.razorpayOrderId }} />
    <div className="payment-outcome-actions"><button type="button" className="member-payment-primary" onClick={onRefresh} disabled={busy}>{busy ? 'Checking…' : 'Refresh status'}</button><button type="button" className="member-payment-secondary" onClick={onRetry}>Back to plans</button></div>
  </section>
}

function PaymentFacts({ payment }) {
  return <dl className="payment-facts">
    <div><dt>Status</dt><dd><StatusBadge status={payment.status} /></dd></div>
    <div><dt>Payment ID</dt><dd>{payment.razorpayPaymentId || payment.paymentId || '—'}</dd></div>
    <div><dt>Order ID</dt><dd>{payment.razorpayOrderId || '—'}</dd></div>
    <div><dt>Plan</dt><dd>{payment.planName || '—'}</dd></div>
    <div><dt>Amount</dt><dd>{formatAmount(payment.amountPaise, payment.currency)}</dd></div>
    <div><dt>Transaction date</dt><dd>{formatDate(payment.verifiedAt || payment.createdAt)}</dd></div>
    {payment.membershipId && <div><dt>Membership ID</dt><dd>#{payment.membershipId}</dd></div>}
  </dl>
}

function PaymentHistory({ onViewPlans }) {
  const [page, setPage] = useState(0)
  const [history, setHistory] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selected, setSelected] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailNotice, setDetailNotice] = useState('')

  async function loadHistory(requestedPage = page, signal) {
    setLoading(true)
    setError('')
    try {
      const result = await getPaymentHistory({ page: requestedPage, size: PAGE_SIZE, signal })
      if (!signal?.aborted) {
        setHistory(result)
        setPage(requestedPage)
      }
    } catch (requestError) {
      if (requestError.name !== 'AbortError' && !signal?.aborted) setError(messageFor(requestError))
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }

  useEffect(() => {
    const controller = new AbortController()
    loadHistory(page, controller.signal)
    return () => controller.abort()
  }, [page])

  async function viewPayment(item) {
    setDetailLoading(true)
    setError('')
    setDetailNotice('')
    // Show the already-authorized history row immediately. The detail endpoint
    // may not have that record yet (for example, if a payment was just created).
    setSelected(item)
    try { setSelected(await getPaymentDetails(item.paymentId)) }
    catch (requestError) {
      setDetailNotice(requestError.status === 404
        ? 'Full payment details are not available right now. The history record is shown below.'
        : 'Could not load additional payment details. The history record is shown below.')
    } finally { setDetailLoading(false) }
  }

  if (selected) return <section className="payment-history-view">
    <button type="button" className="payment-back-button" onClick={() => setSelected(null)}><ArrowLeft size={16} />Back to payment history</button>
    <h3>Payment details</h3>
    {detailNotice && <div className="payment-detail-notice" role="status">{detailNotice}</div>}
    <PaymentFacts payment={selected} />
    {selected.failureReason && <p className="payment-failure-reason">{selected.failureReason}</p>}
  </section>

  return <section className="payment-history-view">
    <div className="payment-history-heading"><div><h3>Payment history</h3><p>Only transactions returned for your signed-in member account are shown.</p></div><button type="button" className="payment-back-button" onClick={onViewPlans}>Membership plans</button></div>
    {error && <div className="member-error" role="alert">{error}<button type="button" onClick={() => loadHistory(page)}>Retry</button></div>}
    {loading || detailLoading ? <div className="member-loading" role="status"><span className="loading-spinner" />{detailLoading ? 'Loading payment details…' : 'Loading payment history…'}</div> : history?.content?.length ? <>
      <div className="member-table-scroll payment-history-table"><table><thead><tr><th>PAYMENT</th><th>PLAN</th><th>AMOUNT</th><th>STATUS</th><th>DATE</th><th></th></tr></thead><tbody>{history.content.map((item) => <tr key={item.paymentId}><td>#{item.paymentId}</td><td>{item.planName}</td><td>{formatAmount(item.amountPaise, item.currency)}</td><td><StatusBadge status={item.status} /></td><td>{formatDate(item.verifiedAt || item.createdAt)}</td><td><button type="button" className="payment-detail-button" onClick={() => viewPayment(item)}><ExternalLink size={14} />Details</button></td></tr>)}</tbody></table></div>
      <div className="payment-pagination"><span>Page {page + 1} of {Math.max(1, history.totalPages || 1)}</span><div><button type="button" className="member-payment-secondary" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Previous</button><button type="button" className="member-payment-secondary" disabled={page + 1 >= (history.totalPages || 1)} onClick={() => setPage((value) => value + 1)}>Next</button></div></div>
    </> : !error ? <div className="payment-history-empty"><CalendarDays size={23} /><strong>No payment records yet</strong><p>Your verified payments will appear here.</p><button type="button" className="member-payment-primary" onClick={onViewPlans}>Browse membership plans</button></div> : null}
  </section>
}
