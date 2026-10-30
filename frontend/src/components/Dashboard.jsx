import {
  Activity,
  ArrowRight,
  CreditCard,
  Dumbbell,
  Users,
} from "lucide-react";

import Metric from "./Metric";
import MemberTable from "./MemberTable";

export default function Dashboard({ members, memberCount, membersLoading, membersError, membershipCount, membershipUnavailable, trainerCount, trainerUnavailable, monthlyRevenue, revenueUnavailable, onRetry, navigate }) {
  const revenueValue = monthlyRevenue ? new Intl.NumberFormat('en-IN', { style: 'currency', currency: monthlyRevenue.currency || 'INR', maximumFractionDigits: 2 }).format(Number(monthlyRevenue.amountPaise || 0) / 100) : '—'
  const revenueNote = revenueUnavailable ? 'Payment data unavailable' : monthlyRevenue ? `${monthlyRevenue.successfulPaymentCount} successful payments this month` : 'Loading payment data'
  return (
    <div className="page-content">
      {/* Welcome banner */}
      <section className="welcome-banner">
        <div className="welcome-content">
          <span className="eyebrow">FORM & FIT CLUB</span>
          <h1>Welcome back. <span aria-hidden="true">👋</span></h1>
          <p>
            A healthier club starts with great management.
            Here's your club's activity at a glance.
          </p>

        </div>

        <div className="welcome-image">
          <img
            src="/images/gym-banner.jpg"
            alt="Member training in a modern gym"
          />
          <div className="image-caption">
            <span>YOUR CLUB</span>
            <strong>Built around better health.</strong>
          </div>
        </div>
      </section>

      {/* Dashboard metrics */}
      <section className="metrics-grid">
        <Metric
          label="Registered members"
          value={membersLoading ? '…' : memberCount ?? '—'}
          icon={Users}
          note={membersError ? 'Member Service unavailable' : 'All member records'}
        />

        <Metric
          label="Active memberships"
          value={membershipCount ?? '—'}
          icon={CreditCard}
          note={membershipUnavailable ? 'Membership Service unavailable' : membershipCount === null ? 'Loading membership data' : 'Current active plans'}
        />

        <Metric
          label="Training team"
          value={trainerCount ?? '—'}
          icon={Dumbbell}
          note={trainerUnavailable ? 'Trainer Service unavailable' : trainerCount === null ? 'Loading trainer data' : 'Trainer records'}
        />

        <Metric
          label="Monthly revenue"
          value={revenueValue}
          icon={Activity}
          note={revenueNote}
        />
      </section>

      {/* Members and operations */}
      <section className="dashboard-grid dashboard-grid-single">
        <article className="surface members-panel">
          <div className="section-header">
            <div>
              <h2>Member directory</h2>
              <p>Sorted by member ID; creation dates are not provided by the API</p>
            </div>

            <button
              className="text-link"
              onClick={() => navigate("Members")}
            >
              View all <ArrowRight size={15} />
            </button>
          </div>

          {membersError ? <div className="member-error" role="alert">{membersError}<button type="button" className="text-link" onClick={onRetry}>Retry</button></div> : membersLoading ? <div className="loading-state" role="status"><span className="loading-spinner" />Loading member records…</div> : <MemberTable members={members.slice(0, 4)} compact />}
        </article>

      </section>

      <p className="page-footer">
        Form &amp; Fit Club <span>·</span> Internal management
      </p>
    </div>
  );
}
