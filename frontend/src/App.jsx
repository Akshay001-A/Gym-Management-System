import { useEffect, useState } from 'react'
import { Plus, Search, X } from 'lucide-react'
import Sidebar from './components/Sidebar'
import Topbar from './components/Topbar'
import Dashboard from './components/Dashboard'
import MemberTable from './components/MemberTable'
import MemberDialog from './components/MemberDialog'
import MembershipsPage from './components/MembershipsPage'
import TrainersPage from './components/TrainersPage'
import SettingsPage from './components/SettingsPage'
import HelpSupportPage from './components/HelpSupportPage'
import RevenuePage from './components/RevenuePage'
import { createMember, deleteMember, getMember, getMembersPage, searchMembers, updateMember } from './api/members'
import { getOverviewCounts } from './api/overview'
import { assignMembershipPlan } from './api/memberships'

const PAGE_SIZE = 10
const COMPACT_TABLES_KEY = 'form-fit-compact-tables'

function readCompactTablesPreference() {
  try {
    return window.localStorage.getItem(COMPACT_TABLES_KEY) === 'true'
  } catch {
    return false
  }
}

export default function App() {
  const [page, setPage] = useState('Overview')
  const [members, setMembers] = useState([])
  const [memberCount, setMemberCount] = useState(null)
  const [loadedKey, setLoadedKey] = useState('')
  const [membersError, setMembersError] = useState('')
  const [memberPage, setMemberPage] = useState(0)
  const [memberTotalPages, setMemberTotalPages] = useState(0)
  const [search, setSearch] = useState('')
  const [searchMatches, setSearchMatches] = useState(null)
  const [searchTotal, setSearchTotal] = useState(0)
  const [overviewCounts, setOverviewCounts] = useState({ membershipCount: null, trainerCount: null, membershipUnavailable: false, trainerUnavailable: false, monthlyRevenue: null, revenueUnavailable: false })
  const [dialog, setDialog] = useState(null)
  const [dialogSaving, setDialogSaving] = useState(false)
  const [dialogError, setDialogError] = useState('')
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [notice, setNotice] = useState('')
  const [mobileNav, setMobileNav] = useState(false)
  const [refreshKey, setRefreshKey] = useState(0)
  const [compactTables, setCompactTables] = useState(readCompactTablesPreference)

  const requestKey = page === 'Overview' ? `overview:${refreshKey}` : `members:${memberPage}:${search.trim()}:${refreshKey}`
  const membersLoading = (page === 'Overview' || page === 'Members') && loadedKey !== requestKey

  useEffect(() => {
    if (page === 'Overview') {
      const controller = new AbortController()
      getMembersPage({ page: 0, size: 4, signal: controller.signal })
        .then((result) => {
          setMembers(result.content)
          setMemberCount(result.totalElements)
        })
        .catch((error) => { if (error.name !== 'AbortError') setMembersError(error.message) })
        .finally(() => { if (!controller.signal.aborted) setLoadedKey(requestKey) })
      getOverviewCounts({ signal: controller.signal })
        .then((result) => setOverviewCounts({ membershipCount: result.activeMemberships, trainerCount: result.trainerCount, membershipUnavailable: result.membershipUnavailable, trainerUnavailable: result.trainerUnavailable, monthlyRevenue: result.monthlyRevenue, revenueUnavailable: result.revenueUnavailable }))
        .catch(() => { if (!controller.signal.aborted) setOverviewCounts((current) => ({ ...current, monthlyRevenue: null, revenueUnavailable: true })) })
      return () => controller.abort()
    }

    if (page !== 'Members') return undefined
    const controller = new AbortController()
    let timer
    const load = async () => {
      try {
        if (search.trim()) {
          const result = await searchMembers(search, { signal: controller.signal })
          setSearchMatches(result.content)
          setSearchTotal(result.totalElements)
          setMemberTotalPages(Math.ceil(result.content.length / PAGE_SIZE))
          setMembers(result.content.slice(memberPage * PAGE_SIZE, (memberPage + 1) * PAGE_SIZE))
          setMemberCount(result.totalElements)
        } else {
          setSearchMatches(null)
          const result = await getMembersPage({ page: memberPage, size: PAGE_SIZE, signal: controller.signal })
          setMembers(result.content)
          setMemberCount(result.totalElements)
          setMemberTotalPages(result.totalPages)
        }
      } catch (error) {
        if (error.name !== 'AbortError') setMembersError(error.message)
      } finally {
        if (!controller.signal.aborted) setLoadedKey(requestKey)
      }
    }

    if (search.trim()) timer = setTimeout(load, 250)
    else load()
    return () => { clearTimeout(timer); controller.abort() }
  }, [page, memberPage, search, refreshKey, requestKey])

  function navigate(label) {
    setPage(label)
    setSearch('')
    setMemberPage(0)
    setSearchMatches(null)
    setMembersError('')
    if (label === 'Overview') {
      setMembers([])
      setMemberCount(null)
      setOverviewCounts({ membershipCount: null, trainerCount: null, membershipUnavailable: false, trainerUnavailable: false, monthlyRevenue: null, revenueUnavailable: false })
    }
    setMobileNav(false)
  }

  function changeCompactTables(value) {
    setCompactTables(value)
    try {
      window.localStorage.setItem(COMPACT_TABLES_KEY, String(value))
      setNotice('Display preference saved in this browser.')
    } catch {
      setNotice('This browser could not save the display preference. It will apply until you leave the page.')
    }
  }

  function startAddMember() {
    setPage('Members')
    setMemberPage(0)
    setSearch('')
    setDialog({ mode: 'add', member: null })
    setDialogError('')
  }

  async function openDetails(member) {
    setDialog({ mode: 'details', member: null, loading: true, id: member.id })
    setDialogError('')
    try {
      const detail = await getMember(member.id)
      setDialog({ mode: 'details', member: detail, loading: false })
    } catch (error) {
      setDialog({ mode: 'details', member, loading: false })
      setDialogError(error.message)
    }
  }

  function openEdit(member) {
    setDialog({ mode: 'edit', member })
    setDialogError('')
  }

  async function saveMember(payload) {
    if (dialogSaving || !dialog) return
    setDialogSaving(true)
    setDialogError('')
    let createdMember = false
    try {
      if (dialog.mode === 'edit') await updateMember(dialog.member.id, payload)
      else {
        const { plan, ...memberPayload } = payload
        const created = await createMember(memberPayload)
        createdMember = true
        if (plan) await assignMembershipPlan(created.id, plan)
      }
      setDialog(null)
      setNotice(dialog.mode === 'edit' ? 'Member updated.' : 'Member added.')
      setMemberPage(0)
      setRefreshKey((key) => key + 1)
    } catch (error) {
      if (createdMember) {
        setDialog(null)
        setNotice(`Member was created, but the plan could not be assigned: ${error.message}`)
        setRefreshKey((key) => key + 1)
      } else setDialogError(error.message)
    } finally {
      setDialogSaving(false)
    }
  }

  async function confirmDelete() {
    if (!deleteTarget || deleting) return
    setDeleting(true)
    try {
      await deleteMember(deleteTarget.id)
      setNotice(`Member ${deleteTarget.name || `#${deleteTarget.id}`} deleted.`)
      setDeleteTarget(null)
      setMemberPage(0)
      setRefreshKey((key) => key + 1)
    } catch (error) {
      setNotice(`Could not delete member: ${error.message}`)
    } finally {
      setDeleting(false)
    }
  }

  const displayedMembers = searchMatches
    ? searchMatches.slice(memberPage * PAGE_SIZE, (memberPage + 1) * PAGE_SIZE)
    : members
  const displayTotalPages = searchMatches ? Math.ceil(searchMatches.length / PAGE_SIZE) : memberTotalPages

  return (
    <div className={`app-shell ${compactTables ? 'compact-tables' : ''}`}>
      <Sidebar page={page} membersCount={memberCount ?? '—'} mobileNav={mobileNav} setMobileNav={setMobileNav} navigate={navigate} setNotice={setNotice} />
      <main className="main-area">
        <Topbar page={page} setMobileNav={setMobileNav} setNotice={setNotice} />
        {notice && <div className="notice" role="status"><span>{notice}</span><button aria-label="Dismiss notice" onClick={() => setNotice('')}><X size={16} /></button></div>}

        {page === 'Overview' && <Dashboard
          members={members}
          memberCount={memberCount}
          membersLoading={membersLoading}
          membersError={membersError}
          membershipCount={overviewCounts.membershipCount}
          membershipUnavailable={overviewCounts.membershipUnavailable}
          trainerCount={overviewCounts.trainerCount}
          trainerUnavailable={overviewCounts.trainerUnavailable}
          monthlyRevenue={overviewCounts.monthlyRevenue}
          revenueUnavailable={overviewCounts.revenueUnavailable}
          onRetry={() => setRefreshKey((key) => key + 1)}
          navigate={navigate}
        />}

        {page === 'Members' && (
          <div className="page-content">
            <div className="page-title-row"><div><p className="eyebrow">CLUB DIRECTORY</p><h1>Members</h1><p className="page-subtitle">Manage member records and contact details from the Member Service.</p></div><button className="button button-primary" onClick={startAddMember}><Plus size={17} /> Add member</button></div>
            <section className="surface directory-panel">
              <div className="directory-toolbar"><div><h2>All members</h2><p>{search.trim() ? `${searchMatches?.length ?? '…'} matches${searchTotal ? ` from ${searchTotal} records` : ''}` : `${memberCount ?? '…'} total records`}</p></div>
                <label className="search-box"><Search size={17} /><input aria-label="Search members" value={search} onChange={(event) => { setMembersError(''); setSearch(event.target.value); setMemberPage(0) }} placeholder="Search name, email, mobile, or ID…" />{search && <button type="button" aria-label="Clear search" onClick={() => { setMembersError(''); setSearch(''); setMemberPage(0) }}><X size={15} /></button>}</label>
              </div>
              {membersError ? <div className="member-error" role="alert">{membersError}<button type="button" className="button button-secondary" onClick={() => setRefreshKey((key) => key + 1)}>Retry</button></div> : membersLoading ? <div className="loading-state" role="status"><span className="loading-spinner" />Loading members…</div> : <MemberTable members={displayedMembers} onView={openDetails} onEdit={openEdit} onDelete={setDeleteTarget} />}
              {!membersLoading && !membersError && displayTotalPages > 1 && <div className="pagination"><span>Page {memberPage + 1} of {displayTotalPages}</span><div><button className="button button-secondary" onClick={() => setMemberPage((current) => Math.max(0, current - 1))} disabled={memberPage === 0}>Previous</button><button className="button button-secondary" onClick={() => setMemberPage((current) => Math.min(displayTotalPages - 1, current + 1))} disabled={memberPage + 1 >= displayTotalPages}>Next</button></div></div>}
            </section>
          </div>
        )}

        {page === 'Memberships' && <MembershipsPage setNotice={setNotice} />}

        {page === 'Trainers' && <TrainersPage setNotice={setNotice} />}

        {page === 'Revenue' && <RevenuePage />}

        {page === 'Settings' && <SettingsPage compactTables={compactTables} onCompactTablesChange={changeCompactTables} />}

        {page === 'Help & Support' && <HelpSupportPage />}
      </main>
      {dialog && <MemberDialog key={`${dialog.mode}-${dialog.id ?? dialog.member?.id ?? 'new'}-${dialog.loading ? 'loading' : 'ready'}`} mode={dialog.mode} member={dialog.member} loading={dialog.loading} saving={dialogSaving} error={dialogError} onClose={() => { if (!dialogSaving) { setDialog(null); setDialogError('') } }} onSave={saveMember} />}
      {deleteTarget && <div className="modal-backdrop" role="presentation"><section className="member-form confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="delete-title" aria-describedby="delete-description"><div className="modal-heading"><div><h2 id="delete-title">Delete member?</h2><p id="delete-description">Delete {deleteTarget.name || `member #${deleteTarget.id}`} from the Member Service? This cannot be undone.</p></div></div>{notice.startsWith('Could not delete') && <div className="member-error" role="alert">{notice}</div>}<div className="modal-actions"><button type="button" className="button button-secondary" onClick={() => setDeleteTarget(null)} disabled={deleting}>Cancel</button><button type="button" className="button button-danger" onClick={confirmDelete} disabled={deleting}>{deleting ? 'Deleting…' : 'Delete member'}</button></div></section></div>}
    </div>
  )
}
