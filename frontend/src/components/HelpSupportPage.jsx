import { BookOpenCheck, CircleHelp } from 'lucide-react'

const topics = [
  {
    title: 'Overview',
    body: 'Use the dashboard for registered member totals, active memberships, and trainer totals when their services are available. Revenue remains unavailable because this backend does not provide payment or revenue records.',
  },
  {
    title: 'Members',
    body: 'Search by name, email, mobile number, or member ID. Add, view, edit, and delete records using the Member Service. The directory is sorted by ID because the API does not provide a creation date.',
  },
  {
    title: 'Memberships',
    body: 'Assign MONTHLY, QUARTERLY, or YEARLY plans to an existing member. The Membership Service sets the plan price and dates. It maintains membership status and marks expired records according to its own rules.',
  },
  {
    title: 'Trainers',
    body: 'Search and filter trainer records by details, availability, or status. Add, view, edit, and delete trainer profiles through the Trainer Service.',
  },
  {
    title: 'Loading and errors',
    body: 'A loading message means the page is waiting for a service response. Empty states mean the service returned no matching records. If a service is unavailable, check that PostgreSQL, the relevant service, and the API Gateway are running, then retry.',
  },
]

export default function HelpSupportPage() {
  return (
    <div className="page-content">
      <div className="page-title-row"><div><p className="eyebrow">GUIDE</p><h1>Help &amp; Support</h1><p className="page-subtitle">How to use the Form &amp; Fit Club management workspace.</p></div></div>
      <section className="surface help-intro"><span className="empty-icon"><BookOpenCheck size={23} /></span><div><h2>Using the workspace</h2><p>Choose a section from the sidebar. Pages that manage records send requests through the API Gateway to the existing backend services.</p></div></section>
      <div className="help-topics">{topics.map((topic) => <article className="surface help-topic" key={topic.title}><span className="help-topic-icon"><CircleHelp size={18} /></span><div><h2>{topic.title}</h2><p>{topic.body}</p></div></article>)}</div>
      <p className="page-footer">No separate support contact is configured in this project.</p>
    </div>
  )
}
