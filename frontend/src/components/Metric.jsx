export default function Metric({ label, value, icon: Icon, note }) {
  return (
    <article className="metric-card">
      <div className="metric-top"><span>{label}</span><Icon size={18} strokeWidth={1.7} /></div>
      <div className="metric-value">{value}</div>
      <div className="metric-note"><span className="metric-note-dot" />{note}</div>
    </article>
  );
}
