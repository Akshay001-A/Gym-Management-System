import { MonitorCog } from 'lucide-react'

export default function SettingsPage({ compactTables, onCompactTablesChange }) {
  return (
    <div className="page-content">
      <div className="page-title-row"><div><p className="eyebrow">PREFERENCES</p><h1>Settings</h1><p className="page-subtitle">Adjust display preferences for this browser.</p></div></div>
      <section className="surface settings-panel">
        <div className="settings-section-heading"><span className="empty-icon"><MonitorCog size={22} /></span><div><h2>Display</h2><p>These preferences affect how tables appear in this browser.</p></div></div>
        <label className="setting-toggle"><span><strong>Compact tables</strong><small>Use less vertical space in member, membership, and trainer tables.</small></span><input type="checkbox" checked={compactTables} onChange={(event) => onCompactTablesChange(event.target.checked)} /></label>
        <p className="settings-storage-note">Saved locally in this browser only. This preference is not sent to or synchronized with the backend.</p>
      </section>
    </div>
  )
}
