import {
  ChevronDown, CircleHelp, CreditCard, Dumbbell, LayoutDashboard,
  Settings, ShieldCheck, Users, X,
} from "lucide-react";

const navItems = [
  { label: "Overview", icon: LayoutDashboard },
  { label: "Members", icon: Users },
  { label: "Memberships", icon: CreditCard },
  { label: "Trainers", icon: Dumbbell },
];

export default function Sidebar({ page, membersCount, mobileNav, setMobileNav, navigate }) {
  return (
    <>
      <aside className={`sidebar ${mobileNav ? "sidebar-open" : ""}`}>
        <a className="brand" href="#" onClick={(event) => event.preventDefault()}>
          <span className="brand-mark"><Dumbbell size={22} strokeWidth={2.2} /></span>
          <span className="brand-copy"><strong>FORM<span> & </span>FIT</strong><small>CLUB MANAGEMENT</small></span>
          <button className="mobile-close" aria-label="Close navigation" onClick={() => setMobileNav(false)}><X size={19} /></button>
        </a>
        <div className="club-selector">
          <div className="club-avatar">F</div>
          <div className="club-copy"><strong>Form & Fit Club</strong><small>Fitness centre</small></div>
          <ChevronDown size={16} />
        </div>
        <p className="nav-heading">WORKSPACE</p>
        <nav className="main-nav">
          {navItems.map(({ label, icon: Icon }) => (
            <button key={label} className={`nav-item ${page === label ? "nav-active" : ""}`} onClick={() => navigate(label)}>
              <Icon size={18} strokeWidth={1.8} />
              <span>{label}</span>
              {label === "Members" && <span className="nav-count">{membersCount}</span>}
            </button>
          ))}
        </nav>
        <p className="nav-heading tools-heading">PREFERENCES</p>
        <button className={`nav-item ${page === 'Settings' ? 'nav-active' : ''}`} aria-current={page === 'Settings' ? 'page' : undefined} onClick={() => navigate('Settings')}><Settings size={18} strokeWidth={1.8} /><span>Settings</span></button>
        <button className={`nav-item ${page === 'Help & Support' ? 'nav-active' : ''}`} aria-current={page === 'Help & Support' ? 'page' : undefined} onClick={() => navigate('Help & Support')}><CircleHelp size={18} strokeWidth={1.8} /><span>Help & support</span></button>
        <div className="sidebar-bottom">
          <div className="plan-note"><ShieldCheck size={19} /><div><strong>Management console</strong><p>Gym operations workspace</p></div></div>
          <div className="account"><div className="account-avatar">A</div><div className="account-copy"><strong>Administrator</strong><small>Club manager</small></div><ChevronDown size={15} /></div>
        </div>
      </aside>
      {mobileNav && <button className="mobile-backdrop" aria-label="Close menu" onClick={() => setMobileNav(false)} />}
    </>
  );
}
